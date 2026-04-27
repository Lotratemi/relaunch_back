package com.codingfactory.routes

import com.codingfactory.models.RelaunchModels
import com.codingfactory.services.StreakService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.streakRoutes(service: StreakService) {
    route("/streaks") {

        get("/{userId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            call.respond(service.getAllForUser(userId))
        }

        get("/{userId}/{streakId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user id")
            val streakId = call.parameters["streakId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid streak id")

            val streak = service.getByIdForUser(streakId, userId)
                ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(streak)
        }

        post {
            val created = service.create(call.receive<RelaunchModels.Streak>())
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Failed to create streak")

            call.respond(HttpStatusCode.Created, created)
        }

        put("/{userId}/{streakId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid user id")
            val streakId = call.parameters["streakId"]?.toLong()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid streak id")

            val updated = service.update(streakId, userId, call.receive())
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Failed to update streak")

            call.respond(updated)
        }

        delete("/{userId}/{streakId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid user id")
            val streakId = call.parameters["streakId"]?.toLong()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid streak id")

            service.delete(streakId, userId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
