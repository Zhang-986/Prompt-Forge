/**
 * Unified LLM Stream Chunk
 * 
 * Represents a single chunk from the LLM stream, typed to allow
 * differentiated frontend rendering of reasoning vs content.
 */
export interface StreamChunk {
    /** Chunk type: REASONING = deep thinking, CONTENT = formal answer */
    type: 'REASONING' | 'CONTENT' | 'TOOL_CALL' | 'DONE'
    /** The actual text content */
    content: string
}

/**
 * Parse raw SSE event into StreamChunk
 */
export function parseStreamChunk(eventType: string, data: string): StreamChunk | null {
    switch (eventType) {
        case 'REASONING':
            return { type: 'REASONING', content: data }
        case 'CONTENT':
            return { type: 'CONTENT', content: data }
        case 'TOOL_CALL':
            return { type: 'TOOL_CALL', content: data }
        case 'DONE':
            return { type: 'DONE', content: '' }
        default:
            return null
    }
}
