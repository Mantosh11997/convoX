package com.convox.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: Long = 0L,
    @SerialName("chat_id") val chatId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val seen: Boolean = false
)
