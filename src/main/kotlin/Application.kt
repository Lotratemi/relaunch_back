package com.codingfactory

import com.codingfactory.repositories.ConversationRepository
import com.codingfactory.repositories.ObjectiveRepository
import com.codingfactory.repositories.ProfilingRepository
import com.codingfactory.repositories.UserRepository
import com.codingfactory.services.CoachService
import com.codingfactory.services.ObjectiveService
import com.codingfactory.services.ProfilingService
import com.codingfactory.services.UserService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }

    configureMonitoring()

    val userService = UserService(UserRepository())
    val objectiveService = ObjectiveService(ObjectiveRepository())
    val coachService = CoachService(
        ConversationRepository(), mistralClient, mistralApiUrl, mistralApiKey, mistralAiModel
    )
    val profilingService = ProfilingService(
        ProfilingRepository(), mistralClient, mistralApiUrl, mistralApiKey, mistralAiModel
    )

    configureRouting(userService, objectiveService, coachService, profilingService)
}
