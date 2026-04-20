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

fun Route.streakRoutes() {
    route("/streaks") {

        // Gets all user's streak
        get("/{userId}") {
            val userId: Long = call.parameters["userId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            val streaks: List<RelaunchModels.Streak> =
                supabase.from("streak")
                    .select {
                        filter { RelaunchModels.Streak::userId eq userId }
                    }.decodeList<RelaunchModels.Streak>()

            call.respond(streaks)
        }

        // Gets a specific user's streak
        get("/{userId}/{streakId}") {
            val userId: Long = call.parameters["userId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            val streakId: Long = call.parameters["streakId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid streak id")

            val streak: RelaunchModels.Streak? = supabase.from("streak")
                .select {
                    filter {
                        RelaunchModels.Streak::id eq streakId
                        RelaunchModels.Streak::userId eq userId
                    }
                }.decodeSingleOrNull()

            if (streak == null) return@get call.respond(HttpStatusCode.NotFound)

            call.respond(streak)
        }

        // Creates a new streak
        post {
            val createdStreak: RelaunchModels.Streak? =
                supabase.from("streak")
                    .insert(call.receive<RelaunchModels.Streak>()) {
                        select()
                    }.decodeSingleOrNull()

            if (createdStreak == null) {
                return@post call.respond(HttpStatusCode.BadRequest, "Failed to create streak")
            }

            call.respond(HttpStatusCode.Created, createdStreak)
        }

        // Updates a specific streak
        put("/{userId}/{streakId}") {
            val userId: Long = call.parameters["userId"]?.toLong()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            val streakId: Long = call.parameters["streakId"]?.toLong()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid streak id")

            val updatedStreak: RelaunchModels.Streak? =
                supabase.from("streak")
                    .update(call.receive()) {
                    select()
                    filter {
                        RelaunchModels.Streak::id eq streakId
                        RelaunchModels.Streak::userId eq userId
                    }
            }.decodeSingleOrNull()

            if (updatedStreak == null) {
                return@put call.respond(HttpStatusCode.BadRequest, "Failed to update streak")
            }

            call.respond(updatedStreak)
        }

        // Deletes a specific streak
        delete("/{userId}/{streakId}") {
            val userId: Long = call.parameters["userId"]?.toLong()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            val streakId: Long = call.parameters["streakId"]?.toLong()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid streak id")

            supabase.from("streak")
                .delete {
                    filter {
                        RelaunchModels.Streak::id eq streakId
                        RelaunchModels.Streak::userId eq userId
                    }
                }

            call.respond(HttpStatusCode.NoContent)
        }

    }
}
