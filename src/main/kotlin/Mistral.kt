package com.codingfactory

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val mistralApiKey: String = System.getenv("MISTRAL_API_KEY")
val mistralApiUrl: String = System.getenv("MISTRAL_API_URL")
val mistralAiModel: String = System.getenv("MISTRAL_AI_MODEL")

val mistralClient: HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}