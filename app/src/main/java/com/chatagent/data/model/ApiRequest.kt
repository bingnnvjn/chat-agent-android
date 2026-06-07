package com.chatagent.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.7,
    val max_tokens: Int = 4096,
    val chat_template_kwargs: ChatTemplateKwargs? = null,
    val tools: List<Tool>? = null,
    val tool_choice: ToolChoice? = null
)

@Serializable
data class ChatTemplateKwargs(
    val enable_thinking: Boolean = false
)

@Serializable
data class ApiMessage(
    val role: String,
    val content: String
) {
    companion object {
        fun text(role: String, content: String) = ApiMessage(role, content)
        fun textWithImage(role: String, text: String, imageUrl: String): ApiMessage =
            ApiMessage(role, text) // image_url handled separately
    }
}

// --------------- 工具调用 ---------------

@Serializable
data class Tool(
    val type: String = "function",
    val function: ToolFunction
)

@Serializable
data class ToolFunction(
    val name: String,
    val description: String = "",
    val parameters: JsonObject? = null
)

@Serializable
data class ToolChoice(
    val type: String = "function",
    val function: ToolChoiceFunction? = null
)

@Serializable
data class ToolChoiceFunction(
    val name: String
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

@Serializable
data class ToolCallFunction(
    val name: String,
    val arguments: String
)

// --------------- 响应 ---------------

@Serializable
data class ChatResponse(
    val id: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null
)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: ApiMessage? = null,
    val delta: Delta? = null,
    val finish_reason: String? = null
)

@Serializable
data class Delta(
    val content: String? = null,
    val reasoning_content: String? = null,
    val tool_calls: List<ToolCall>? = null
)

@Serializable
data class Usage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
)
