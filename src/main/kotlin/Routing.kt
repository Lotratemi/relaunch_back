package com.codingfactory

import com.codingfactory.routes.coachRoutes
import com.codingfactory.routes.conversationRoutes
import com.codingfactory.routes.messageRoutes
import com.codingfactory.routes.objectiveRoutes
import com.codingfactory.routes.streakRoutes
import com.codingfactory.routes.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        userRoutes()
        conversationRoutes()
        messageRoutes()
        coachRoutes()
        objectiveRoutes()
        streakRoutes()
    }
}
