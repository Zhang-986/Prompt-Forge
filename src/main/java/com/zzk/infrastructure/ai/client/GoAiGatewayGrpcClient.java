package com.zzk.infrastructure.ai.client;

import com.zzk.infrastructure.ai.adapter.StreamChunk;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Go AI Gateway gRPC 客户端
 *
 * <p>
 * 封装与 Go AI Gateway 的 gRPC 通信，提供与现有 DynamicLlmClientFactory 兼容的接口。
 * Java 端通过此客户端远程调用 Go 服务获取 AI 流式结果，从而摆脱 WebFlux 直连 AI 厂商的依赖。
 *
 * <p>
 * 使用方式：注入此 Bean 后调用 generateStreamChunks() 即可获得 Flux<StreamChunk>，
 * 与现有 DynamicLlmClientFactory.generateStreamChunks() 返回类型完全一致。
 *
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class GoAiGatewayGrpcClient {

    @Value("${go-ai-gateway.grpc.host:localhost}")
    private String grpcHost;

    @Value("${go-ai-gateway.grpc.port:9090}")
    private int grpcPort;

    private ManagedChannel channel;
    private com.zzk.infrastructure.ai.grpc.proto.AiGatewayGrpc.AiGatewayStub asyncStub;
    private com.zzk.infrastructure.ai.grpc.proto.AiGatewayGrpc.AiGatewayBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder
                .forAddress(grpcHost, grpcPort)
                .usePlaintext() // 内网通信不需要 TLS
                .build();

        asyncStub = com.zzk.infrastructure.ai.grpc.proto.AiGatewayGrpc.newStub(channel);
        blockingStub = com.zzk.infrastructure.ai.grpc.proto.AiGatewayGrpc.newBlockingStub(channel);

        log.info("[GoGrpcClient] 连接 Go AI Gateway gRPC: {}:{}", grpcHost, grpcPort);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("[GoGrpcClient] gRPC 连接已关闭");
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 流式生成（返回 Flux<StreamChunk>，与 DynamicLlmClientFactory 兼容）
     *
     * <p>
     * 通过 gRPC Server Streaming 调用 Go AI Gateway，
     * 将 gRPC 的 StreamObserver 回调模型桥接为 Reactor 的 Flux 响应式流。
     *
     * @param configId 用户模型配置 ID
     * @param prompt   用户提示词
     * @return 响应式 StreamChunk 流
     */
    public Flux<StreamChunk> generateStreamChunks(long configId, String prompt) {
        return Flux.create(sink -> {
            com.zzk.infrastructure.ai.grpc.proto.StreamRequest request = com.zzk.infrastructure.ai.grpc.proto.StreamRequest.newBuilder()
                    .setConfigId(configId)
                    .setPrompt(prompt)
                    .build();

            asyncStub.generateStream(request, new StreamObserver<com.zzk.infrastructure.ai.grpc.proto.StreamChunk>() {
                @Override
                public void onNext(com.zzk.infrastructure.ai.grpc.proto.StreamChunk grpcChunk) {
                    // 将 gRPC StreamChunk 转换为 Java 端的 StreamChunk
                    StreamChunk chunk = convertToStreamChunk(grpcChunk);
                    if (chunk != null) {
                        sink.next(chunk);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.error("[GoGrpcClient] 流式生成错误: {}", t.getMessage());
                    sink.error(t);
                }

                @Override
                public void onCompleted() {
                    sink.complete();
                }
            });
        });
    }

    /**
     * 流式生成（返回 Flux<String>，兼容旧接口）
     *
     * @param configId 用户模型配置 ID
     * @param prompt   用户提示词
     * @return 响应式文本流（仅 CONTENT 类型）
     */
    public Flux<String> generateStream(long configId, String prompt) {
        return generateStreamChunks(configId, prompt)
                .filter(chunk -> chunk.type() == StreamChunk.ChunkType.CONTENT
                        || chunk.type() == StreamChunk.ChunkType.REASONING)
                .map(StreamChunk::content);
    }

    /**
     * 获取可用模型列表
     *
     * @param configId 配置 ID
     * @return 模型名称列表
     */
    public List<String> fetchAvailableModels(long configId) {
        com.zzk.infrastructure.ai.grpc.proto.ModelsRequest request = com.zzk.infrastructure.ai.grpc.proto.ModelsRequest.newBuilder()
                .setConfigId(configId)
                .build();

        com.zzk.infrastructure.ai.grpc.proto.ModelsResponse response = blockingStub.fetchAvailableModels(request);
        return response.getModelsList();
    }

    /**
     * 验证模型连通性
     *
     * @param configId 配置 ID
     * @return 验证结果
     */
    public com.zzk.infrastructure.ai.grpc.proto.ValidateResponse validateModel(long configId) {
        com.zzk.infrastructure.ai.grpc.proto.ValidateRequest request = com.zzk.infrastructure.ai.grpc.proto.ValidateRequest.newBuilder()
                .setConfigId(configId)
                .build();

        return blockingStub.validateModel(request);
    }

    /**
     * 转发 Chat Completion 请求到 Go AI Gateway
     *
     * <p>
     * 用于 Function Calling 场景：Java 构建完整的 OpenAI 格式请求体（含 messages + tools），
     * 通过 gRPC 发送给 Go，Go 负责查配置 + HTTP 转发到 AI 厂商，返回原始 JSON 响应。
     *
     * @param configId        用户模型配置 ID
     * @param requestBodyJson 完整的 OpenAI 格式请求体 JSON
     * @return AI 厂商的原始 JSON 响应
     */
    public String forwardChatCompletion(long configId, String requestBodyJson) {
        com.zzk.infrastructure.ai.grpc.proto.ForwardChatRequest request = com.zzk.infrastructure.ai.grpc.proto.ForwardChatRequest
                .newBuilder()
                .setConfigId(configId)
                .setRequestBodyJson(requestBodyJson)
                .build();

        com.zzk.infrastructure.ai.grpc.proto.ForwardChatResponse response = blockingStub.forwardChatCompletion(request);

        return response.getResponseBodyJson();
    }

    /**
     * 将 gRPC 的 StreamChunk 转换为 Java 端的 StreamChunk
     */
    private StreamChunk convertToStreamChunk(com.zzk.infrastructure.ai.grpc.proto.StreamChunk grpcChunk) {
        com.zzk.infrastructure.ai.grpc.proto.ChunkType grpcType = grpcChunk.getChunkType();
        String content = grpcChunk.getContent();

        return switch (grpcType) {
            case REASONING -> StreamChunk.reasoning(content);
            case CONTENT -> StreamChunk.content(content);
            case TOOL_CALL -> StreamChunk.toolCall(content);
            case DONE -> StreamChunk.done();
            default -> StreamChunk.content(content);
        };
    }
}
