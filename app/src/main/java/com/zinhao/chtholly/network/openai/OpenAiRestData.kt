package com.zinhao.chtholly.network.openai

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double? = null,
    val top_p: Double? = null,
    val max_tokens: Int? = null,
    val max_completion_tokens: Int? = null,
    val stream: Boolean = false,
    val response_format: ResponseFormat? = null,
    val reasoning_effort: String? = null,
    val tools: List<Tool>? = null
)

data class Tool(
    val type: String = "function",
    val function: FunctionDefinition
)

data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: ToolParameters
)

data class ToolParameters(
    val type: String = "object",
    val properties: Map<String, ToolProperty>,
    val required: List<String>? = null
)

data class ToolProperty(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)

data class ChatMessage(
    val role: String,     // system / user / assistant
    val content: Any?,  // String 或 List<ContentPart>
//    val tool_calls: List<ToolCall>? = null,
//    val tool_call_id: String? = null
)

data class ChatResponse(
    val id: String?,
    val `object`: String?,
    val created: Long?,
    val model: String?,
    val choices: List<ChatChoice>,
    val error: OpenAIError?
)

data class ChatChoice(
    val message: ChatMessage,
    val finish_reason: String?,
    val index: Int
)

data class OpenAIError(
    val message: String?,
    val type: String?,
    val code: String?
)

sealed class ContentPart {
    data class TextPart(
        val type: String = "text",
        val text: String
    ) : ContentPart()

    data class ImagePart(
        val type: String = "image_url",
        val image_url: ImageUrl
    ) : ContentPart()
}

data class ImageUrl(
    val url: String,
    val detail: String? = null  // "low" / "high" / "auto"
)