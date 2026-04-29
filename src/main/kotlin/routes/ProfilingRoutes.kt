package com.codingfactory.routes

import com.codingfactory.models.ProfilingModels
import com.codingfactory.services.ProfilingService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.profilingRoutes(service: ProfilingService) {
    route("/profiling") {

        // Soumettre les réponses et obtenir le profil calculé
        post {
            val request = call.receive<ProfilingModels.ProfilingRequest>()
            val result = service.analyze(request)
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Failed to analyze profiling")
            call.respond(HttpStatusCode.Created, result)
        }

        // Récupérer le dernier profil d'un utilisateur
        get("/{userId}") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user id")
            val result = service.getLatestProfile(userId)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(result)
        }

        // Récupérer l'historique complet
        get("/{userId}/history") {
            val userId = call.parameters["userId"]?.toLong()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user id")
            call.respond(service.getHistory(userId))
        }
    }
}