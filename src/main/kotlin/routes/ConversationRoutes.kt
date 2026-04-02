package com.codingfactory.routes

import com.codingfactory.models.Conversation
import com.codingfactory.supabase
import io.github.jan.supabase.postgrest.from
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.conversationRoutes() {
    route("/conversations") {
        get {
            val userId = call.request.queryParameters["user_id"]?.toLongOrNull()
            val conversations = supabase.from("conversation").select {
                if (userId != null) filter { eq("user_id", userId) }
            }.decodeList<Conversation>()
            call.respond(conversations)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val conversation = supabase.from("conversation").select {
                filter { eq("id", id) }
            }.decodeSingleOrNull<Conversation>()
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(conversation)
        }

        post {
            val body = call.receive<Conversation>()
            val created = supabase.from("conversation").insert(body) { select() }.decodeSingle<Conversation>()
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val body = call.receive<Conversation>()
            val updated = supabase.from("conversation").update(body) {
                select()
                filter { eq("id", id) }
            }.decodeSingle<Conversation>()
            call.respond(updated)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
            supabase.from("conversation").delete { filter { eq("id", id) } }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
