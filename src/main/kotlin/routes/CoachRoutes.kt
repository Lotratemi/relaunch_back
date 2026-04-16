package com.codingfactory.routes

import com.codingfactory.mistralApiKey
import com.codingfactory.mistralApiUrl
import com.codingfactory.mistralClient
import com.codingfactory.models.MistralModels.ConversationAppendRequest
import com.codingfactory.models.MistralModels.ConversationCreateRequest
import com.codingfactory.models.MistralModels.ConversationResponse
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private fun ConversationResponse.replyText(): String =
    outputs.firstOrNull { it.type == "message.output" }?.content.orEmpty()

fun Route.coachRoutes() {
    route("/coach") {
        post("/chat") {
            val response: ConversationResponse = mistralClient.post("$mistralApiUrl/v1/conversations") {
                contentType(ContentType.Application.Json)
                bearerAuth(mistralApiKey)
                setBody(ConversationCreateRequest(inputs = call.receiveText(), model = "mistral-small-latest"))
            }.body()
            call.respondText("${response.conversation_id}\n${response.replyText()}")
        }

        post("/chat/{conversationId}") {
            val conversationId = call.parameters["conversationId"]!!
            val response: ConversationResponse = mistralClient.post("$mistralApiUrl/v1/conversations/$conversationId") {
                contentType(ContentType.Application.Json)
                bearerAuth(mistralApiKey)
                setBody(ConversationAppendRequest(inputs = call.receiveText()))
            }.body()
            call.respondText(response.replyText())
        }
    }
}
