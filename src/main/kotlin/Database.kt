package com.codingfactory

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.Json
import java.sql.Connection

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

    // Pooled JDBC connections for the TEST database. A new raw connection per
    // request exhausts Postgres' max_connections under load; the pool caps and
    // reuses a small set instead.
    private val dataSource: HikariDataSource by lazy {
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:postgresql://${env("TEST_DB_HOST")}:${env("TEST_DB_PORT")}/${env("TEST_DB_NAME")}"
                username = env("TEST_DB_USER")
                password = env("TEST_DB_PASSWORD")
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 10
                poolName = "relaunch-test-pool"
            }
        )
    }

    fun connection(): Connection {
        return try {
            dataSource.connection
        } catch (e: Exception) {
            throw RuntimeException("Erreur connexion DB Test : ${e.message}")
        }
    }

    suspend fun <T> run(prod: suspend (SupabaseClient) -> T, test: (Connection) -> T): T {
        return try {
            when (mode) {
                DbMode.PROD -> prod(supabase)
                DbMode.TEST -> connection().use(test)
            }
        } catch (e: Exception) {
            println("Erreur DataBase (${mode}): ${e.message}")
            throw e
        }

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
