package com.codingfactory.routes

import com.codingfactory.models.RelaunchModels
import com.codingfactory.services.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.userRoutes(service: UserService) {
    route("/users") {

        get("/{userId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            val user = service.getById(userId)
                ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(user)
        }

        post {
            val created = service.create(call.receive<RelaunchModels.User>())
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{userId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            val updated = service.update(userId, call.receive())
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Failed to update user")

            call.respond(updated)
        }

        delete("/{userId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            service.delete(userId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
