package grpcserver

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net"

	"go-ai-gateway/factory"
	"go-ai-gateway/grpcserver/pb"
	"go-ai-gateway/model"
	"go-ai-gateway/repository"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// AiGatewayServer gRPC 服务实现
// 供 Java 服务通过 gRPC 调用 Go AI Gateway 的 AI 能力
type AiGatewayServer struct {
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
func (s *AiGatewayServer) GenerateStream(req *pb.StreamRequest, stream grpc.ServerStream) error {
	if req.Prompt == "" {
		return status.Error(codes.InvalidArgument, "prompt 不能为空")
	}

	cfg, err := s.resolveConfig(req.ConfigID, req.UserID, req.Provider)
	if err != nil {
		return status.Errorf(codes.NotFound, "%v", err)
	}

	log.Printf("[gRPC] GenerateStream: userId=%d, provider=%s, model=%s",
		req.UserID, cfg.Provider, cfg.GetEffectiveModelName())

	ctx := stream.Context()
	contentCh, errCh := s.factory.GenerateStream(ctx, cfg, req.Prompt)

	// 逐 chunk 发送
	for chunk := range contentCh {
		grpcChunk := &pb.StreamChunk{Content: chunk.Content}
		if err := stream.SendMsg(grpcChunk); err != nil {
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
	if req.ConfigID <= 0 {
		return nil, status.Error(codes.InvalidArgument, "config_id 不能为空")
	}

	cfg, err := s.repo.GetConfigByID(req.ConfigID)
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
	if req.ConfigID <= 0 {
		return nil, status.Error(codes.InvalidArgument, "config_id 不能为空")
	}

	cfg, err := s.repo.GetConfigByID(req.ConfigID)
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
	// 注意: 完整的 gRPC 服务注册需要 protoc-gen-go-grpc 生成的
	// RegisterAiGatewayServer() 函数。当前先启动 gRPC server 并保留服务实例。
	// 安装 protoc 后: pb.RegisterAiGatewayServer(grpcSrv, srv)
	srv := NewAiGatewayServer(f, repo)
	_ = srv

	log.Printf("[gRPC] 服务启动: %s (等待 protoc 注册完成后提供完整服务)", addr)
	return grpcSrv.Serve(lis)
}
