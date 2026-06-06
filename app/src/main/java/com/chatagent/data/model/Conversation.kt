package com.chatagent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String = "",
    val title: String = "新对话",
    val messages: List<Message> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
