package com.codingfactory.services

import com.codingfactory.mistralApiKey
import com.codingfactory.mistralApiUrl
import com.codingfactory.mistralAiModel
import com.codingfactory.mistralClient
import com.codingfactory.models.MistralModels
import com.codingfactory.models.ProfilingModels.ProfileDimension
import com.codingfactory.models.ProfilingModels.ProfilingAnswer
import com.codingfactory.models.ProfilingModels.ProfilingRequest
import com.codingfactory.models.ProfilingModels.ProfilingResponse
import com.codingfactory.models.ProfilingModels.UserProfile
import com.codingfactory.repositories.ProfilingRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class ProfilingService(
    private val repo: ProfilingRepository,
    private val mistralClient: HttpClient,
    private val mistralApiUrl: String,
    private val mistralApiKey: String,
    private val mistralAiModel: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun analyze(request: ProfilingRequest): ProfilingResponse? {

        // 1. Construire le texte des réponses à envoyer à Mistral
        val answersText = request.answers.entries
            .sortedBy { it.key }
            .joinToString("\n") { (id, value) -> "Q$id: $value" }

        // 2. Appel Mistral — même pattern que CoachService.createConversation()
        val mistralResponse: MistralModels.ConversationResponse =
            mistralClient.post("$mistralApiUrl/v1/conversations") {
                contentType(ContentType.Application.Json)
                bearerAuth(mistralApiKey)
                setBody(
                    MistralModels.ConversationCreateRequest(
                        inputs       = answersText,
                        instructions = MistralModels.PROFILING_INSTRUCTIONS,
                        completion_args = MistralModels.PROFILING_COMPLETION_ARGS,
                        model        = mistralAiModel
                    )
                )
            }.body()

        // 3. Parser la réponse JSON structurée
        val content = mistralResponse.outputs
            .firstOrNull { it.type == "message.output" }
            ?.content
            ?: return null

        val parsed = json.decodeFromString<MistralModels.ProfilingAnalysisResponse>(content)

        // 4. Persister le profil
        val profile = repo.saveProfile(
            UserProfile(
                userId      = request.userId,
                profileType = parsed.profile_type,
                version     = request.version,
                rawScores   = json.encodeToString(
                    MapSerializer(String.serializer(), Float.serializer()),
                    parsed.raw_scores
                )
            )
        ) ?: return null

        // 5. Persister les réponses brutes
        repo.saveAnswers(
            request.answers.map { (questionId, answerValue) ->
                ProfilingAnswer(
                    userProfileId = profile.id!!,
                    questionId    = questionId,
                    questionText  = "",   // à enrichir si vous avez la liste des questions côté back
                    answerValue   = answerValue
                )
            }
        )

        // 6. Persister les dimensions
        val dimensions = parsed.dimensions.map {
            ProfileDimension(
                userProfileId = profile.id!!,
                dimension     = it.dimension,
                score         = it.score,
                label         = it.label
            )
        }
        repo.saveDimensions(dimensions)

        return ProfilingResponse(profile = profile, dimensions = dimensions)
    }

    suspend fun getLatestProfile(userId: Long): ProfilingResponse? {
        val profile = repo.findLatestByUserId(userId) ?: return null
        val dimensions = repo.findDimensionsByProfileId(profile.id!!)
        return ProfilingResponse(profile = profile, dimensions = dimensions)
    }

    suspend fun getHistory(userId: Long): List<UserProfile> {
        return repo.findHistoryByUserId(userId)
    }
}