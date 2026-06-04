package com.codingfactory

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.Json
import java.net.URI
import java.sql.Connection

enum class DbMode { PROD, TEST }

object Database {

    private val mode: DbMode = if (System.getenv("APP_MODE") == "test") DbMode.TEST else DbMode.PROD

    @PublishedApi internal val json = Json { ignoreUnknownKeys = true }

    private fun env(name: String) = System.getenv(name) ?: error("Missing $name environment variable")

    private data class Conn(
        val host: String, val port: String, val name: String,
        val user: String, val password: String,
    )

    // Render exposes the database as a single connection string, e.g.
    //   postgresql://user:pass@dpg-xxxx-a[:5432]/dbname
    // (the internal URL often omits the port). Parse it into discrete parts so we
    // can hand HikariCP a clean jdbc:postgresql:// URL plus credentials.
    private fun parseUrl(url: String): Conn {
        val uri = URI(url.removePrefix("jdbc:"))
        val userInfo = uri.userInfo?.split(":", limit = 2)
            ?: error("DATABASE_URL is missing credentials")
        val host = uri.host ?: error("DATABASE_URL is missing a host")
        val port = if (uri.port != -1) uri.port.toString() else "5432"
        val name = uri.path.removePrefix("/").ifEmpty { error("DATABASE_URL is missing a database name") }
        return Conn(host, port, name, userInfo[0], userInfo.getOrElse(1) { "" })
    }

    // Both PROD (managed Postgres on Render) and TEST (local Docker Postgres) are
    // reached over plain JDBC through a single Hikari pool. A new raw connection
    // per request would exhaust Postgres' max_connections under load; the pool
    // caps and reuses a small set instead.
    private val dataSource: HikariDataSource by lazy {
        val c = when (mode) {
            // Prefer a single DATABASE_URL (the Render idiom); fall back to discrete vars.
            DbMode.PROD -> System.getenv("DATABASE_URL")?.let { parseUrl(it) }
                ?: Conn(env("DB_HOST"), env("DB_PORT"), env("DB_NAME"), env("DB_USER"), env("DB_PASSWORD"))
            DbMode.TEST -> Conn(env("TEST_DB_HOST"), env("TEST_DB_PORT"), env("TEST_DB_NAME"), env("TEST_DB_USER"), env("TEST_DB_PASSWORD"))
        }
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:postgresql://${c.host}:${c.port}/${c.name}"
                username = c.user
                password = c.password
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 10
                poolName = "relaunch-pool"
            }
        )
    }

    fun connection(): Connection {
        return try {
            dataSource.connection
        } catch (e: Exception) {
            throw RuntimeException("Erreur connexion DB ($mode) : ${e.message}")
        }
    }

    // Applies the schema (idempotent CREATE TABLE IF NOT EXISTS) at startup so a
    // fresh Render database bootstraps itself on first deploy — no manual psql.
    fun initSchema() {
        val ddl = Database::class.java.getResource("/schema.sql")?.readText()
            ?: error("schema.sql not found on classpath")
        connection().use { conn ->
            conn.createStatement().use { it.execute(ddl) }
        }
    }

    suspend fun <T> run(block: (Connection) -> T): T {
        return try {
            connection().use(block)
        } catch (e: Exception) {
            println("Erreur DataBase ($mode): ${e.message}")
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
