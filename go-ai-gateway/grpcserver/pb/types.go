package pb

// 手写 gRPC 消息类型（等价于 protoc 生成的代码）
// 对应 ai_gateway.proto 中的 message 定义

// StreamRequest 流式生成请求
type StreamRequest struct {
	UserID   int64  `json:"user_id"`
	Provider string `json:"provider"`
	Prompt   string `json:"prompt"`
	ConfigID int64  `json:"config_id"`
}

// StreamChunk 流式响应片段
type StreamChunk struct {
	Content string `json:"content"`
}

// ModelsRequest 可用模型请求
type ModelsRequest struct {
	ConfigID int64 `json:"config_id"`
}

// ModelsResponse 可用模型响应
type ModelsResponse struct {
	Models []string `json:"models"`
}

// ValidateRequest 模型验证请求
type ValidateRequest struct {
	ConfigID int64 `json:"config_id"`
}

// ValidateResponse 模型验证响应
type ValidateResponse struct {
	Success bool   `json:"success"`
	Message string `json:"message"`
}
