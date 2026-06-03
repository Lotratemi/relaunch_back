package com.codingfactory.mock

import com.codingfactory.models.MistralModels
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.concurrent.atomic.AtomicLong

/**
 * Stand-in for the Mistral conversations API, used during load tests so we
 * exercise OUR back (Ktor + Postgres) without hitting / paying Mistral.
 *
 * Speaks just enough of the real contract:
 *   POST /v1/conversations         -> returns a conversation_id + a canned profiling output
 *   POST /v1/conversations/{id}    -> returns a canned coach output
 *
 * Run it with:  ./gradlew runMistralMock      (defaults to :8089)
 * Then point the app at it:  MISTRAL_API_URL=http://localhost:8089
 *
 * The `content` field of each output is itself a JSON string, matching the
 * structured-output schemas the services decode (CoachResponse / ProfilingAnalysisResponse).
 */

private val json = Json { encodeDefaults = true }
private val counter = AtomicLong(0)

// CoachResponse JSON, as a string (what /coach/chat decodes).
private val coachContent: String = json.encodeToString(
    buildJsonObject {
        put("response", "Quel est ton principal blocage en ce moment ?")
        put("objectives_creation_trigger", false)
        putJsonArray("objectives") {}
    }
)

// ProfilingAnalysisResponse JSON, as a string (what /profiling decodes).
private val profilingContent: String = json.encodeToString(
    buildJsonObject {
        put("profile_type", "Le Visionnaire")
        putJsonObject("raw_scores") {
            put("Leadership", 0.8); put("Empathie", 0.5); put("Analyse", 0.6)
            put("Créativité", 0.7); put("Structure", 0.4); put("Adaptabilité", 0.6)
        }
        putJsonArray("dimensions") {
            add(buildJsonObject { put("dimension", "Leadership"); put("score", 0.8); put("label", "Élevé") })
            add(buildJsonObject { put("dimension", "Empathie"); put("score", 0.5); put("label", "Modéré") })
            add(buildJsonObject { put("dimension", "Analyse"); put("score", 0.6); put("label", "Modéré") })
        }
    }
)

private fun envelope(content: String): MistralModels.ConversationResponse =
    MistralModels.ConversationResponse(
        conversation_id = "mock-conv-${counter.incrementAndGet()}",
        outputs = listOf(
            MistralModels.OutputEntry(type = "message.output", role = "assistant", content = content)
        ),
        usage = MistralModels.ConversationUsage(prompt_tokens = 1, completion_tokens = 1, total_tokens = 2),
    )

fun main() {
    val port = System.getenv("MISTRAL_MOCK_PORT")?.toIntOrNull() ?: 8089
    embeddedServer(Netty, port = port) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
        routing {
            // Coach.createConversation reads only conversation_id; Profiling.analyze reads
            // outputs[].content. We return both so a single endpoint serves both callers.
            post("/v1/conversations") {
                call.respond(envelope(profilingContent))
            }
            post("/v1/conversations/{id}") {
                call.respond(envelope(coachContent))
            }
        }
    }.start(wait = true)
}
