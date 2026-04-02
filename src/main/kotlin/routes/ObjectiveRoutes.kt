package com.codingfactory.routes

import com.codingfactory.models.Objective
import com.codingfactory.supabase
import io.github.jan.supabase.postgrest.from
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.objectiveRoutes() {
    route("/objectives") {
        get {
            val userId = call.request.queryParameters["user_id"]?.toLongOrNull()
            val objectives = supabase.from("objectives").select {
                if (userId != null) filter { eq("user_id", userId) }
            }.decodeList<Objective>()
            call.respond(objectives)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val objective = supabase.from("objectives").select {
                filter { eq("id", id) }
            }.decodeSingleOrNull<Objective>()
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(objective)
        }

        post {
            val body = call.receive<Objective>()
            val created = supabase.from("objectives").insert(body) { select() }.decodeSingle<Objective>()
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val body = call.receive<Objective>()
            val updated = supabase.from("objectives").update(body) {
                select()
                filter { eq("id", id) }
            }.decodeSingle<Objective>()
            call.respond(updated)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
            supabase.from("objectives").delete { filter { eq("id", id) } }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
