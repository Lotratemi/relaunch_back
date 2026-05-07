package com.codingfactory

import com.codingfactory.routes.coachRoutes
import com.codingfactory.routes.objectiveRoutes
import com.codingfactory.routes.profilingRoutes
import com.codingfactory.routes.userRoutes
import com.codingfactory.services.CoachService
import com.codingfactory.services.ObjectiveService
import com.codingfactory.services.ProfilingService
import com.codingfactory.services.UserService
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(
    userService: UserService,
    objectiveService: ObjectiveService,
    coachService: CoachService,
    profilingService: ProfilingService
) {
    routing {
        userRoutes(userService)
        coachRoutes(coachService)
        objectiveRoutes(objectiveService)
        profilingRoutes(profilingService)
    }
}
