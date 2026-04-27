package com.codingfactory

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private fun env(name: String) = System.getenv(name) ?: error("Missing $name environment variable")

val mistralApiKey: String = env("MISTRAL_API_KEY")
val mistralApiUrl: String = env("MISTRAL_API_URL")
val mistralAiModel: String = env("MISTRAL_AI_MODEL")

val mistralClient: HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
