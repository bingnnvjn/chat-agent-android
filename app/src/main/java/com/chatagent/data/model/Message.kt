package com.chatagent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = "",
    val role: String = "user",
    val content: String = "",
    val image: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
