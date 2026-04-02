package com.codingfactory

import com.codingfactory.routes.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        userRoutes()
        conversationRoutes()
        messageRoutes()
        objectiveRoutes()
        streakRoutes()
    }
}
