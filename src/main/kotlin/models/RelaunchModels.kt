package com.codingfactory.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

class RelaunchModels {
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
        @SerialName("mistral_conv_id") val mistralConvId: String,
    )

    @Serializable
    data class CoachResponse(
        @SerialName("response")
        val response: String,

        @SerialName("objectives_creation_trigger")
        val objectivesCreationTrigger: Boolean,

        @SerialName("objectives")
        val objectives: List<String>
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
}
