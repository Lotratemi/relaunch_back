package com.codingfactory

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*

fun main(args: Array<String>) {
    embeddedServer(
        Netty,
        port = 31337,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    supabase
    configureRouting()
}
