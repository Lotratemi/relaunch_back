package com.codingfactory.routes

import com.codingfactory.mistralAiModel
import com.codingfactory.mistralApiKey
import com.codingfactory.mistralApiUrl
import com.codingfactory.mistralClient
import com.codingfactory.models.MistralModels
import com.codingfactory.models.RelaunchModels
import com.codingfactory.supabase
import io.github.jan.supabase.postgrest.from
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private fun MistralModels.ConversationResponse.replyText(): String =
    outputs.firstOrNull { it.type == "message.output" }?.content.orEmpty()

fun Route.coachRoutes() {
    route("/coach") {

        // Create a new Mistral conversation, stores ids in the database and returns the conversation id
        post("/chat/{userId}") {
            val userId: Long = call.parameters["userId"]?.toLong()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Failed to parse user id")

            val mistralResponse: MistralModels.ConversationResponse =
                mistralClient.post("$mistralApiUrl/v1/conversations") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(mistralApiKey)
                    setBody(
                        MistralModels.ConversationCreateRequest(
                            inputs = "", // We send an empty prompt because it's required by Mistral API
                            model = mistralAiModel
                        )
                    )
                }.body()

            val createdRelaunchConversation: RelaunchModels.Conversation? =
                supabase.from("conversation").insert(
                    RelaunchModels.Conversation(
                        userId = userId,
                        mistralConvId = mistralResponse.conversation_id
                    )
                ) { select() }.decodeSingle<RelaunchModels.Conversation>()

            if (createdRelaunchConversation == null) {
                return@post call.respond(HttpStatusCode.BadRequest, "Failed to create conversation")
            }

            call.respond(createdRelaunchConversation)
        }

        // Appends the user prompt to the specified Mistral conversation and returns model's response
        post("/chat/{userId}/{conversationId}") {
            val userId: Long = call.parameters["userId"]?.toLong()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Failed to parse user id")

            val conversationId: String = call.parameters["conversationId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Failed to parse conversation id")

            val targetedConversation: RelaunchModels.Conversation? =
                supabase.from("conversation").select {
                    filter {
                        RelaunchModels.Conversation::userId eq userId
                        RelaunchModels.Conversation::mistralConvId eq conversationId
                    }
            }.decodeSingleOrNull<RelaunchModels.Conversation>()

            if (targetedConversation == null) {
                return@post call.respond(HttpStatusCode.BadRequest, "Failed to find conversation")
            }

            val mistralResponse: MistralModels.ConversationResponse =
                mistralClient.post("$mistralApiUrl/v1/conversations/$conversationId") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(mistralApiKey)
                    setBody(MistralModels.ConversationAppendRequest(inputs = call.receiveText()))
                }.body()

            call.respondText(mistralResponse.replyText())
        }
    }
}
