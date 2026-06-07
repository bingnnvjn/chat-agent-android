package com.chatagent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.7,
    val max_tokens: Int = 4096,
    val chat_template_kwargs: ChatTemplateKwargs? = null
)

@Serializable
data class ChatTemplateKwargs(
    val enable_thinking: Boolean = false
)

@Serializable
data class ApiMessage(
    val role: String,
    val content: String
)

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
    val content: String? = null
)

@Serializable
data class Usage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
)
