package com.codingfactory

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json

val testJson = Json { ignoreUnknownKeys = true }

fun testApp(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
    application { module() }
    val client = createClient {
        install(ContentNegotiation) { json(testJson) }
    }
    block(client)
}
