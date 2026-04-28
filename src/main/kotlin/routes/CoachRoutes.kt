package com.codingfactory.routes

import com.codingfactory.services.CoachService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.coachRoutes(service: CoachService) {
    route("/coach") {

        post("/chat/{userId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Failed to parse user id")

            call.respond(service.createConversation(userId))
        }

        post("/chat/{userId}/{conversationId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Failed to parse user id")
            val conversationId = call.parameters["conversationId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Failed to parse conversation id")

            val reply = service.chat(userId, conversationId, call.receiveText())
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Failed to find conversation")

            call.respond(reply)
        }
    }
}
