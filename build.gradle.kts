import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.codingfactory"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

fun loadLocalProps(): Map<String, String> {
    val file = file("local.properties")
    if (!file.exists()) return emptyMap()
    val props = Properties().apply { load(file.inputStream()) }
    return props.entries.associate { it.key.toString() to it.value.toString() }
}

tasks.named<JavaExec>("run") {
    environment(loadLocalProps())
}

// Runs the back against the LOCAL test Postgres (APP_MODE=test) with Mistral
// pointed at the local mock — the right setup for load testing without touching
// prod (Supabase) or paying Mistral. Brings up the test DB first.
//   1. ./gradlew runMistralMock   (separate terminal)
//   2. ./gradlew runTestMode
tasks.register<JavaExec>("runTestMode") {
    group = "application"
    description = "Runs the app in test mode (local Postgres + mocked Mistral) for load testing."
    mainClass.set("io.ktor.server.netty.EngineMain")
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn("waitForTestDb")   // brings up postgres:16-alpine on :5433 with init.sql
    environment(loadLocalProps())
    environment("APP_MODE", "test")
    // Default Mistral to the local mock; override by exporting MISTRAL_API_URL beforehand.
    environment("MISTRAL_API_URL", System.getenv("MISTRAL_API_URL") ?: "http://localhost:8089")
}

// Stand-in Mistral conversations API (see src/main/kotlin/mock/MistralMock.kt).
tasks.register<JavaExec>("runMistralMock") {
    group = "application"
    description = "Runs the local Mistral mock server (default :8089) for load testing."
    mainClass.set("com.codingfactory.mock.MistralMockKt")
    classpath = sourceSets["main"].runtimeClasspath
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(platform("io.github.jan-tennert.supabase:bom:${libs.versions.supabase.get()}"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation(libs.ktor.client.content.negotiation)
    implementation("org.postgresql:postgresql:42.7.4")
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.hikaricp)

    testImplementation(libs.ktor.server.test.host)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${libs.versions.kotlin.get()}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

val props = loadLocalProps()
fun prop(key: String) = props[key] ?: error("Missing $key in local.properties")
val testDbContainer = prop("TEST_DB_CONTAINER")
val testDbPort = prop("TEST_DB_PORT")
val testDbName = prop("TEST_DB_NAME")
val testDbUser = prop("TEST_DB_USER")
val testDbPassword = prop("TEST_DB_PASSWORD")

val initSqlPath = file("src/test/resources/init.sql").absolutePath.replace('\\', '/')

val startTestDb by tasks.registering(Exec::class) {
    description = "Starts the Postgres container used by integration tests."
    isIgnoreExitValue = true
    commandLine(
        "docker", "run", "-d", "--rm",
        "--name", testDbContainer,
        "-p", "$testDbPort:5432",
        "-e", "POSTGRES_DB=$testDbName",
        "-e", "POSTGRES_USER=$testDbUser",
        "-e", "POSTGRES_PASSWORD=$testDbPassword",
        "-v", "$initSqlPath:/docker-entrypoint-initdb.d/init.sql:ro",
        "postgres:16-alpine"
    )
}

val waitForTestDb by tasks.registering {
    description = "Waits until the Postgres test container is ready."
    dependsOn(startTestDb)
    doLast {
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            val proc = ProcessBuilder("docker", "exec", testDbContainer, "pg_isready", "-U", testDbUser, "-d", testDbName)
                .redirectErrorStream(true).start()
            proc.inputStream.readAllBytes()
            if (proc.waitFor() == 0) return@doLast
            Thread.sleep(500)
        }
        throw GradleException("Test database not ready after 30s")
    }
}

val stopTestDb by tasks.registering(Exec::class) {
    description = "Stops the Postgres test container."
    isIgnoreExitValue = true
    commandLine("docker", "rm", "-f", testDbContainer)
}

tasks.withType<Test> {
    useJUnitPlatform()
    dependsOn(waitForTestDb)
    finalizedBy(stopTestDb)
    environment(loadLocalProps())
    environment("APP_MODE", "test")
}
