package com.convox.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: String = "",
    @SerialName("user_a") val userA: String = "",
    @SerialName("user_b") val userB: String = "",
    @SerialName("username_a") val usernameA: String = "",
    @SerialName("username_b") val usernameB: String = "",
    @SerialName("last_message") val lastMessage: String = "",
    @SerialName("last_message_time") val lastMessageTime: Long = 0L
) {
    fun otherUid(myUid: String) = if (userA == myUid) userB else userA
    fun otherUsername(myUid: String) = if (userA == myUid) usernameB else usernameA
}
