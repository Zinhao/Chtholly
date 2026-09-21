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

data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

data class ToolCallFunction(
    val name: String,
    val arguments: String
)

data class ChatMessage(
    val role: String,     // system / user / assistant / tool
    val content: Any?,  // String 或 List<ContentPart>
    val tool_calls: List<ToolCall>? = null,
    val tool_call_id: String? = null,
)

data class ChatResponse(
    val id: String?,
    val `object`: String?,
    val created: Long?,
    val model: String?,
    val choices: List<ChatChoice>,
    val usage: Usage,
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

/**
{
    "prompt_tokens": 19,
    "completion_tokens": 10,
    "total_tokens": 29,
    "prompt_tokens_details": {
      "cached_tokens": 0,
      "audio_tokens": 0
    },
    "completion_tokens_details": {
      "reasoning_tokens": 0,
      "audio_tokens": 0,
      "accepted_prediction_tokens": 0,
      "rejected_prediction_tokens": 0
    }
  }
*/
data class PromptTokensDetails(
    val audio_tokens: Int?,
    val cached_tokens: Int?
)

/**
{
    "prompt_tokens": 19,
    "completion_tokens": 10,
    "total_tokens": 29,
    "prompt_tokens_details": {
      "cached_tokens": 0,
      "audio_tokens": 0
    },
    "completion_tokens_details": {
      "reasoning_tokens": 0,
      "audio_tokens": 0,
      "accepted_prediction_tokens": 0,
      "rejected_prediction_tokens": 0
    }
  }
*/
data class Usage(
    val completion_tokens: Int,
    val completion_tokens_details: CompletionTokensDetails?,
    val prompt_tokens: Int,
    val prompt_tokens_details: PromptTokensDetails?,
    val total_tokens: Int
)

/**
{
    "prompt_tokens": 19,
    "completion_tokens": 10,
    "total_tokens": 29,
    "prompt_tokens_details": {
      "cached_tokens": 0,
      "audio_tokens": 0
    },
    "completion_tokens_details": {
      "reasoning_tokens": 0,
      "audio_tokens": 0,
      "accepted_prediction_tokens": 0,
      "rejected_prediction_tokens": 0
    }
  }
*/
data class CompletionTokensDetails(
    val accepted_prediction_tokens: Int?,
    val audio_tokens: Int?,
    val reasoning_tokens: Int?,
    val rejected_prediction_tokens: Int?
)

//data: {"id":"chatcmpl-k9muxp9t9a9s0jpn6dej7",
// "object":"chat.completion.chunk",
// "created":1789957451,
// "model":"qwen3.5-4b-uncensored-hauhaucs-aggressive",
// "system_fingerprint":"qwen3.5-4b-uncensored-hauhaucs-aggressive",
// "choices":[{"index":0,
// "delta":{"content":"“"},
// "logprobs":null,
// "finish_reason":null}]}
// Streaming response data classes
data class StreamChunk(
    val id: String?,
    val `object`: String?,
    val created: Long?,
    val model: String?,
    val choices: List<StreamChoice>?,
    val usage: Usage?
)

data class StreamChoice(
    val index: Int,
    val delta: StreamDelta?,
    val finish_reason: String?
)

data class StreamDelta(
    val role: String?,
    val content: String?
)
