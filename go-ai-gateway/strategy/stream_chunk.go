package strategy

// ChunkType 响应块类型
type ChunkType string

const (
	// ChunkReasoning 深度思考/推理过程
	// OpenAI o1: reasoning_content
	// DeepSeek R1: reasoning_content / reasoning
	// Claude: thinking_delta
	// Gemini: thought
	ChunkReasoning ChunkType = "reasoning"

	// ChunkContent 正式回答内容
	ChunkContent ChunkType = "content"

	// ChunkToolCall 工具调用
	ChunkToolCall ChunkType = "tool_call"

	// ChunkDone 流结束标识
	ChunkDone ChunkType = "done"
)

// StreamChunk 统一流式响应块
// 适配器模式：将各厂商的不同响应格式适配为统一模型
type StreamChunk struct {
	Type    ChunkType `json:"type"`
	Content string    `json:"content"`
}

// Reasoning 创建推理类型的块
func Reasoning(content string) StreamChunk {
	return StreamChunk{Type: ChunkReasoning, Content: content}
}

// Content 创建内容类型的块
func Content(content string) StreamChunk {
	return StreamChunk{Type: ChunkContent, Content: content}
}

// ToolCall 创建工具调用类型的块
func ToolCall(content string) StreamChunk {
	return StreamChunk{Type: ChunkToolCall, Content: content}
}

// Done 创建结束标识块
func Done() StreamChunk {
	return StreamChunk{Type: ChunkDone}
}

// HasContent 判断是否为有效内容块
func (c StreamChunk) HasContent() bool {
	return c.Content != ""
}
