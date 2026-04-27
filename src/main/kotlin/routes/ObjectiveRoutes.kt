package com.codingfactory.routes

import com.codingfactory.models.RelaunchModels
import com.codingfactory.services.ObjectiveService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.objectiveRoutes(service: ObjectiveService) {
    route("/objectives") {

        get("/{userId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            call.respond(service.getAllForUser(userId))
        }

        get("/{userId}/{objectiveId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user id")
            val objectiveId = call.parameters["objectiveId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid objective id")

            val objective = service.getByIdForUser(objectiveId, userId)
                ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(objective)
        }

        post {
            val created = service.create(call.receive<RelaunchModels.Objective>())
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Failed to create objective")

            call.respond(HttpStatusCode.Created, created)
        }

        put("/{userId}/{objectiveId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid user id")
            val objectiveId = call.parameters["objectiveId"]?.toLong()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid objective id")

            val updated = service.update(objectiveId, userId, call.receive())
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Failed to update objective")

            call.respond(updated)
        }

        delete("/{userId}/{objectiveId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid user id")
            val objectiveId = call.parameters["objectiveId"]?.toLong()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid objective id")

            service.delete(objectiveId, userId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
