package grpcserver

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"strings"
	"time"

	"go-ai-gateway/factory"
	"go-ai-gateway/grpcserver/pb"
	"go-ai-gateway/model"
	"go-ai-gateway/repository"
	"go-ai-gateway/strategy"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// AiGatewayServer gRPC 服务实现
// 供 Java 服务通过 gRPC 调用 Go AI Gateway 的 AI 能力
type AiGatewayServer struct {
	pb.UnimplementedAiGatewayServer
	factory *factory.DynamicLlmClientFactory
	repo    *repository.ConfigRepo
}

// NewAiGatewayServer 创建 gRPC 服务
func NewAiGatewayServer(f *factory.DynamicLlmClientFactory, repo *repository.ConfigRepo) *AiGatewayServer {
	return &AiGatewayServer{
		factory: f,
		repo:    repo,
	}
}

// GenerateStream 流式生成 (Server Streaming RPC)
// Java 调用此方法获取 AI 流式结果
func (s *AiGatewayServer) GenerateStream(req *pb.StreamRequest, stream pb.AiGateway_GenerateStreamServer) error {
	if req.Prompt == "" {
		return status.Error(codes.InvalidArgument, "prompt 不能为空")
	}

	cfg, err := s.resolveConfig(req.ConfigId, req.UserId, req.Provider)
	if err != nil {
		return status.Errorf(codes.NotFound, "%v", err)
	}

	log.Printf("[gRPC] GenerateStream: userId=%d, provider=%s, model=%s",
		req.UserId, cfg.Provider, cfg.GetEffectiveModelName())

	ctx := stream.Context()
	contentCh, errCh := s.factory.GenerateStream(ctx, cfg, req.Prompt)

	// 逐 chunk 发送
	for chunk := range contentCh {
		// 转换 ChunkType
		var chunkType pb.ChunkType
		switch chunk.Type {
		case strategy.ChunkReasoning:
			chunkType = pb.ChunkType_REASONING
		case strategy.ChunkContent:
			chunkType = pb.ChunkType_CONTENT
		case strategy.ChunkToolCall:
			chunkType = pb.ChunkType_TOOL_CALL
		case strategy.ChunkDone:
			chunkType = pb.ChunkType_DONE
		default:
			chunkType = pb.ChunkType_CONTENT
		}

		grpcChunk := &pb.StreamChunk{
			ChunkType: chunkType,
			Content:   chunk.Content,
		}
		if err := stream.Send(grpcChunk); err != nil {
			return status.Errorf(codes.Internal, "发送流数据失败: %v", err)
		}
	}

	// 检查错误
	select {
	case err := <-errCh:
		if err != nil {
			return status.Errorf(codes.Internal, "AI 生成失败: %v", err)
		}
	default:
	}

	return nil
}

// FetchAvailableModels 获取用户可用模型列表
func (s *AiGatewayServer) FetchAvailableModels(ctx context.Context, req *pb.ModelsRequest) (*pb.ModelsResponse, error) {
	if req.ConfigId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "config_id 不能为空")
	}

	cfg, err := s.repo.GetConfigByID(req.ConfigId)
	if err != nil {
		return nil, status.Errorf(codes.NotFound, "配置不存在: %v", err)
	}

	// 解析已保存的可用模型列表
	var models []string
	if cfg.AvailableModels.Valid && cfg.AvailableModels.String != "" {
		if err := json.Unmarshal([]byte(cfg.AvailableModels.String), &models); err != nil {
			log.Printf("[gRPC] 解析可用模型失败: %v", err)
			models = []string{cfg.GetEffectiveModelName()}
		}
	} else {
		models = []string{cfg.GetEffectiveModelName()}
	}

	return &pb.ModelsResponse{Models: models}, nil
}

// ValidateModel 验证模型连通性
func (s *AiGatewayServer) ValidateModel(ctx context.Context, req *pb.ValidateRequest) (*pb.ValidateResponse, error) {
	if req.ConfigId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "config_id 不能为空")
	}

	cfg, err := s.repo.GetConfigByID(req.ConfigId)
	if err != nil {
		return &pb.ValidateResponse{
			Success: false,
			Message: fmt.Sprintf("配置不存在: %v", err),
		}, nil
	}

	// 发一条简短 prompt 测试连通性
	contentCh, errCh := s.factory.GenerateStream(ctx, cfg, "Hi")

	// 消费所有输出
	for range contentCh {
	}

	select {
	case err := <-errCh:
		if err != nil {
			return &pb.ValidateResponse{
				Success: false,
				Message: fmt.Sprintf("模型连接失败: %v", err),
			}, nil
		}
	default:
	}

	return &pb.ValidateResponse{
		Success: true,
		Message: "连接成功",
	}, nil
}

// ForwardChatCompletion 转发 Chat Completion 请求到 AI 厂商
// Java 端发送完整的 OpenAI 格式请求体，Go 负责查配置 + HTTP 转发
func (s *AiGatewayServer) ForwardChatCompletion(ctx context.Context, req *pb.ForwardChatRequest) (*pb.ForwardChatResponse, error) {
	if req.ConfigId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "config_id 不能为空")
	}

	cfg, err := s.repo.GetConfigByID(req.ConfigId)
	if err != nil {
		return nil, status.Errorf(codes.NotFound, "配置不存在: %v", err)
	}

	// 构建完整的 API URL
	baseURL := strings.TrimRight(cfg.GetEffectiveBaseURL(), "/")
	modelName := cfg.GetEffectiveModelName()
	chatPath := determineChatEndpoint(cfg.Provider, modelName)
	fullURL := baseURL + chatPath

	log.Printf("[gRPC] ForwardChatCompletion: provider=%s, model=%s, url=%s", cfg.Provider, modelName, fullURL)

	// 构建 HTTP 请求
	httpReq, err := http.NewRequestWithContext(ctx, "POST", fullURL, strings.NewReader(req.RequestBodyJson))
	if err != nil {
		return nil, status.Errorf(codes.Internal, "创建 HTTP 请求失败: %v", err)
	}

	httpReq.Header.Set("Content-Type", "application/json")

	// 根据厂商设置认证头
	switch strings.ToLower(cfg.Provider) {
	case "anthropic", "claude":
		httpReq.Header.Set("x-api-key", cfg.APIKey)
		httpReq.Header.Set("anthropic-version", "2023-06-01")
	default:
		httpReq.Header.Set("Authorization", "Bearer "+cfg.APIKey)
	}

	// 发送请求
	client := &http.Client{Timeout: 120 * time.Second}
	resp, err := client.Do(httpReq)
	if err != nil {
		return nil, status.Errorf(codes.Unavailable, "AI API 调用失败: %v", err)
	}
	defer resp.Body.Close()

	// 读取响应
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, status.Errorf(codes.Internal, "读取 AI 响应失败: %v", err)
	}

	if resp.StatusCode != http.StatusOK {
		log.Printf("[gRPC] ForwardChatCompletion 失败: status=%d, body=%s", resp.StatusCode, string(body))
		return nil, status.Errorf(codes.Internal, "AI API 返回错误 (状态码: %d): %s", resp.StatusCode, string(body))
	}

	log.Printf("[gRPC] ForwardChatCompletion 成功: %d bytes", len(body))
	return &pb.ForwardChatResponse{
		ResponseBodyJson: string(body),
	}, nil
}

// determineChatEndpoint 根据厂商确定 API 端点（复用 OpenAI 策略的逻辑）
func determineChatEndpoint(provider, modelName string) string {
	switch provider {
	case "github":
		return "/chat/completions?api-version=2024-12-01-preview"
	case "azure":
		return "/openai/deployments/" + modelName + "/chat/completions?api-version=2024-02-01"
	case "bedrock":
		return "/model/" + modelName + "/invoke"
	case "zhipu", "hunyuan", "baichuan", "moonshot", "qwen", "aliyun",
		"deepseek", "minimax", "stepfun", "spark", "yi", "sensenova",
		"mistral", "perplexity", "groq", "cohere", "novita",
		"togetherai", "ollama", "openrouter":
		return "/chat/completions"
	default:
		return "/v1/chat/completions"
	}
}

// resolveConfig 根据请求参数解析用户模型配置
func (s *AiGatewayServer) resolveConfig(configID, userID int64, provider string) (*model.UserModelConfig, error) {
	if configID > 0 {
		return s.repo.GetConfigByID(configID)
	}
	if userID > 0 && provider != "" {
		return s.repo.GetConfigByUserAndProvider(userID, provider)
	}
	return nil, fmt.Errorf("需要提供 config_id 或 user_id + provider")
}

// StartGRPCServer 启动 gRPC 服务
func StartGRPCServer(addr string, f *factory.DynamicLlmClientFactory, repo *repository.ConfigRepo) error {
	lis, err := net.Listen("tcp", addr)
	if err != nil {
		return fmt.Errorf("gRPC 监听失败: %w", err)
	}

	grpcSrv := grpc.NewServer()

	// 注册服务实现
	srv := NewAiGatewayServer(f, repo)
	pb.RegisterAiGatewayServer(grpcSrv, srv)

	log.Printf("[gRPC] 服务启动: %s", addr)
	return grpcSrv.Serve(lis)
}
