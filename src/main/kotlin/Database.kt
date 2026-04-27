package com.codingfactory

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.serialization.json.Json
import java.sql.Connection
import java.sql.DriverManager

enum class DbMode { PROD, TEST }

object Database {

    private val mode: DbMode = if (System.getenv("APP_MODE") == "test") DbMode.TEST else DbMode.PROD

    @PublishedApi internal val json = Json { ignoreUnknownKeys = true }

    private fun env(name: String) = System.getenv(name) ?: error("Missing $name environment variable")

    val supabase: SupabaseClient by lazy {
        createSupabaseClient(supabaseUrl = env("SUPABASE_URL"), supabaseKey = env("SUPABASE_KEY")) {
            install(Postgrest)
            install(Auth)
            install(Realtime)
        }
    }

    private val jdbcUrl: String by lazy {
        "jdbc:postgresql://${env("TEST_DB_HOST")}:${env("TEST_DB_PORT")}/${env("TEST_DB_NAME")}"
    }
    private val jdbcUser: String by lazy { env("TEST_DB_USER") }
    private val jdbcPassword: String by lazy { env("TEST_DB_PASSWORD") }

    fun connection(): Connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword)

    suspend fun <T> run(prod: suspend (SupabaseClient) -> T, test: (Connection) -> T): T = when (mode) {
        DbMode.PROD -> prod(supabase)
        DbMode.TEST -> connection().use(test)
    }

    inline fun <reified T> Connection.fetchAll(sql: String, vararg args: Any?): List<T> {
        prepareStatement(sql).use { ps ->
            args.forEachIndexed { i, a -> ps.setObject(i + 1, a) }
            val rs = ps.executeQuery()
            return buildList { while (rs.next()) add(json.decodeFromString<T>(rs.getString(1))) }
        }
    }

    inline fun <reified T> Connection.fetchOne(sql: String, vararg args: Any?): T? =
        fetchAll<T>(sql, *args).firstOrNull()

    fun Connection.exec(sql: String, vararg args: Any?) {
        prepareStatement(sql).use { ps ->
            args.forEachIndexed { i, a -> ps.setObject(i + 1, a) }
            ps.executeUpdate()
        }
    }
}
