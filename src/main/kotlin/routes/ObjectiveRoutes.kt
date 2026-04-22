package com.codingfactory.routes

import com.codingfactory.models.RelaunchModels
import com.codingfactory.supabase
import io.github.jan.supabase.postgrest.from
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.objectiveRoutes() {
    route("/objectives") {

        // Gets all user's objectives
        get("/{userId}") {
            val userId: Long = call.parameters["userId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            val objectives: List<RelaunchModels.Objective> =
                supabase.from("objectives")
                    .select {
                        filter { RelaunchModels.Objective::userId eq userId }
                    }.decodeList<RelaunchModels.Objective>()

            call.respond(objectives)
        }

        // Gets a specific user's objective
        get("/{userId}/{objectiveId}") {
            val userId: Long = call.parameters["id"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            val objectiveId: Long = call.parameters["id"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid objective id")

            val objective: RelaunchModels.Objective? =
                supabase.from("objectives")
                    .select {
                        filter {
                            RelaunchModels.Objective::id eq objectiveId
                            RelaunchModels.Objective::userId eq userId
                        }
                    }.decodeSingleOrNull()

            if (objective == null) return@get call.respond(HttpStatusCode.NotFound)

            call.respond(objective)
        }

        // Creates a new objective
        post {
            val createdObjective: RelaunchModels.Objective? =
                supabase.from("objectives")
                    .insert(call.receive<RelaunchModels.Objective>()) {
                        select()
                    }.decodeSingleOrNull()

            if (createdObjective == null) {
                return@post call.respond(HttpStatusCode.BadRequest, "Failed to create objective")
            }

            call.respond(HttpStatusCode.Created, createdObjective)
        }

        // Updates a specific objective
        put("/{userId}/{objectiveId}") {
            val userId: Long = call.parameters["id"]?.toLong()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            val objectiveId: Long = call.parameters["id"]?.toLong()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid objective id")

            val updatedObjective: RelaunchModels.Objective? =
                supabase.from("objectives")
                    .update(call.receive()) {
                        select(); filter {
                            RelaunchModels.Objective::id eq objectiveId
                            RelaunchModels.Objective::userId eq userId
                        }
                    }.decodeSingleOrNull()

            if (updatedObjective == null) {
                return@put call.respond(HttpStatusCode.BadRequest, "Failed to update objective")
            }

            call.respond(updatedObjective)
        }

        // Deletes a specific objective
        delete("/{userId}/{objectiveId}") {
            val userId: Long = call.parameters["id"]?.toLong()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            val objectiveId: Long = call.parameters["id"]?.toLong()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid objective id")

            supabase.from("objectives")
                .delete {
                    filter { RelaunchModels.Objective::id.eq(objectiveId) }
                    filter { RelaunchModels.Objective::userId.eq(userId) }
                }

            call.respond(HttpStatusCode.NoContent)
        }

    }
}
