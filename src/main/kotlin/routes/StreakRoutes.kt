package com.codingfactory.routes

import com.codingfactory.models.Streak
import com.codingfactory.supabase
import io.github.jan.supabase.postgrest.from
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.streakRoutes() {
    route("/streaks") {
        get {
            val userId = call.request.queryParameters["user_id"]?.toLongOrNull()
            val streaks = supabase.from("streak").select {
                if (userId != null) filter { eq("user_id", userId) }
            }.decodeList<Streak>()
            call.respond(streaks)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val streak = supabase.from("streak").select {
                filter { eq("id", id) }
            }.decodeSingleOrNull<Streak>()
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(streak)
        }

        post {
            val body = call.receive<Streak>()
            val created = supabase.from("streak").insert(body) { select() }.decodeSingle<Streak>()
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")
            val body = call.receive<Streak>()
            val updated = supabase.from("streak").update(body) {
                select()
                filter { eq("id", id) }
            }.decodeSingle<Streak>()
            call.respond(updated)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
            supabase.from("streak").delete { filter { eq("id", id) } }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
