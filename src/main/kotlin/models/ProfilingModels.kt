package com.codingfactory.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ProfilingModels {

    @Serializable
    data class UserProfile(
        val id: Long? = null,
        @SerialName("user_id") val userId: Long,
        @SerialName("profile_type") val profileType: String,
        val version: Int = 1,
        @SerialName("raw_scores") val rawScores: String,  // JSON string
        @SerialName("completed_at") val completedAt: String? = null
    )

    @Serializable
    data class ProfilingAnswer(
        val id: Long? = null,
        @SerialName("user_profile_id") val userProfileId: Long,
        @SerialName("question_id") val questionId: Int,
        @SerialName("question_text") val questionText: String,
        @SerialName("answer_value") val answerValue: String,
        @SerialName("answered_at") val answeredAt: String? = null
    )

    @Serializable
    data class ProfileDimension(
        val id: Long? = null,
        @SerialName("user_profile_id") val userProfileId: Long,
        val dimension: String,
        val score: Float,
        val label: String
    )

    // Objet reçu du front pour déclencher l'analyse
    @Serializable
    data class ProfilingRequest(
        @SerialName("user_id") val userId: Long,
        val answers: Map<Int, String>,
        val version: Int = 1
    )

    // Objet retourné au front après analyse
    @Serializable
    data class ProfilingResponse(
        val profile: UserProfile,
        val dimensions: List<ProfileDimension>
    )
}