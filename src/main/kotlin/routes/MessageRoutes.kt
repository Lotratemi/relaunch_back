package com.codingfactory.routes

import com.codingfactory.models.Message
import com.codingfactory.supabase
import io.github.jan.supabase.postgrest.from
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.messageRoutes() {
    route("/messages") {
        get {
            val conversationId = call.request.queryParameters["conversation_id"]?.toLongOrNull()
            val messages = supabase.from("message").select {
                if (conversationId != null) filter { eq("conversation_id", conversationId) }
            }.decodeList<Message>()
            call.respond(messages)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val message = supabase.from("message").select {
                filter { eq("id", id) }
            }.decodeSingleOrNull<Message>()
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(message)
        }

        post {
            val body = call.receive<Message>()
            val created = supabase.from("message").insert(body) { select() }.decodeSingle<Message>()
            call.respond(HttpStatusCode.Created, created)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
            supabase.from("message").delete { filter { eq("id", id) } }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
