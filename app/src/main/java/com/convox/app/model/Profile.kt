package com.convox.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    val username: String = "",
    @SerialName("username_lower") val usernameLower: String = "",
    @SerialName("last_seen") val lastSeen: Long = 0L,
    val about: String = ""
)
