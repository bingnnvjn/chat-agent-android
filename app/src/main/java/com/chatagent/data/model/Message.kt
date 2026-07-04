package com.chatagent.data.model

import kotlinx.serialization.Serializable

/** 消息类型：普通文本 / 工具调用 / 工具结果 */
enum class MessageType { TEXT, TOOL_CALL, TOOL_RESULT }

@Serializable
data class Message(
    val id: String = "",
    val role: String = "user",
    val content: String = "",
    val thinkingContent: String? = null,
    val image: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    // Agent Tool Calling 字段
    val type: MessageType = MessageType.TEXT,
    val toolCallId: String? = null,
    val toolName: String? = null,
    val toolArgs: String? = null
)
