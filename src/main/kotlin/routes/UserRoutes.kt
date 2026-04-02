package com.codingfactory.routes

import com.codingfactory.models.User
import com.codingfactory.supabase
import io.github.jan.supabase.postgrest.from
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes() {
    route("/users") {
        get {
            val users = supabase.from("user").select().decodeList<User>()
            call.respond(users)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val user = supabase.from("user").select {
                filter { eq("id", id) }
            }.decodeSingleOrNull<User>()
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(user)
        }

        post {
            val body = call.receive<User>()
            val created = supabase.from("user").insert(body) { select() }.decodeSingle<User>()
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val body = call.receive<User>()
            val updated = supabase.from("user").update(body) {
                select()
                filter { eq("id", id) }
            }.decodeSingle<User>()
            call.respond(updated)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
            supabase.from("user").delete { filter { eq("id", id) } }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
