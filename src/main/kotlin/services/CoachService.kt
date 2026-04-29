package com.codingfactory.services

import com.codingfactory.models.MistralModels
import com.codingfactory.models.RelaunchModels.*
import com.codingfactory.repositories.ConversationRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

class CoachService(
    private val repo: ConversationRepository,
    private val mistralClient: HttpClient,
    private val mistralApiUrl: String,
    private val mistralApiKey: String,
    private val mistralAiModel: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createConversation(userId: Long): Conversation {
        return try{
            val mistralResponse: MistralModels.ConversationResponse =
                mistralClient.post("$mistralApiUrl/v1/conversations") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(mistralApiKey)
                    setBody(
                        MistralModels.ConversationCreateRequest(
                            inputs = "",
                            model = mistralAiModel
                        )
                    )
                }.body()

            repo.create(
                Conversation(userId = userId, mistralConvId = mistralResponse.conversation_id)
            )
        } catch (e: Exception){
            throw Exception("ERROR_CREATE_CONV: ${e.message}")
        }
    }

    suspend fun chat(userId: Long, conversationId: String, message: String): CoachResponse? {
        return try{
            val conversation = repo.findByUserIdAndMistralId(userId, conversationId) ?: return null

            val mistralResponse: MistralModels.ConversationResponse =
                mistralClient.post("$mistralApiUrl/v1/conversations/${conversation.mistralConvId}") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(mistralApiKey)
                    setBody(MistralModels.ConversationAppendRequest(inputs = message))
                }.body()

            val content = mistralResponse.outputs
                .firstOrNull { it.type == "message.output" }
                ?.content
                ?: return null

            json.decodeFromString<CoachResponse>(content)
        } catch (e: Exception){
            throw Exception("ERROR_CHAT: ${e.message}")
        }
    }
}
