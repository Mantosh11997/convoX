package com.convox.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendRequest(
    val id: String = "",
    @SerialName("from_uid") val fromUid: String = "",
    @SerialName("from_username") val fromUsername: String = "",
    @SerialName("to_uid") val toUid: String = "",
    @SerialName("to_username") val toUsername: String = "",
    val status: String = "pending"
)
