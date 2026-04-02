package com.codingfactory.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
@Serializable
data class User(
    val id: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val name: String,
    val mail: String,
    val age: Short
)

@Serializable
data class Conversation(
    val id: Long? = null,
    @SerialName("user_id") val userId: Long,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("objective_set") val objectiveSet: Boolean = false
)

@Serializable
data class Message(
    val id: Long? = null,
    @SerialName("conversation_id") val conversationId: Long,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_user") val isUser: Boolean,
    val content: String
)

@Serializable
data class Objective(
    val id: Long? = null,
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("end_at") val endAt: String,
    val frequency: Long,
    val title: String,
    val description: String? = null
)

@Serializable
data class Streak(
    val id: Long? = null,
    @SerialName("user_id") val userId: Long,
    val data: JsonArray,
    @SerialName("started_at") val startedAt: String? = null
)
