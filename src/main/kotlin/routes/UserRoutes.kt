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

fun Route.userRoutes() {
    route("/users") {

        // Gets user's info by id
        get("/{userId}") {
            val userId: Long = call.parameters["user_id"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            val user: RelaunchModels.User? =
                supabase.from("user")
                    .select {
                        filter { RelaunchModels.User::id eq userId }
                    }.decodeSingleOrNull()

            if (user == null) return@get call.respond(HttpStatusCode.NotFound)

            call.respond(user)
        }

        // Creates a user
        post {
            val createdUser: RelaunchModels.User? =
                supabase.from("user")
                    .insert(call.receive<RelaunchModels.User>()) {
                        select()
                    }.decodeSingle<RelaunchModels.User>()

            if (createdUser == null) {
                return@post call.respond(HttpStatusCode.BadRequest, "Failed to create user")
            }

            call.respond(HttpStatusCode.Created, createdUser)
        }

        // Updates a user
        put("/{userId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            val updatedUser: RelaunchModels.User? =
                supabase.from("user")
                    .update(call.receive()) {
                        select()
                        filter { RelaunchModels.User::id eq userId }
                    }.decodeSingleOrNull()

            if (updatedUser == null) {
                return@put call.respond(HttpStatusCode.BadRequest, "Failed to update user")
            }

            call.respond(updatedUser)
        }

        // Deletes a user
        delete("/{userId}") {
            val userId: Long = call.parameters["userId"]?.toLong()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid user id")

            supabase.from("user").delete { filter { RelaunchModels.User::id eq userId } }

            call.respond(HttpStatusCode.NoContent)
        }
    }
}
