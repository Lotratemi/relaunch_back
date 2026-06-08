package com.codingfactory.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
        val description: String? = null,
        @SerialName("is_completed") val isComplet: Boolean = false,
    )
}
