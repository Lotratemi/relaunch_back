package com.codingfactory

import com.codingfactory.models.RelaunchModels.*
import io.github.jan.supabase.postgrest.from
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.collections.all
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StreakRoutesTest {

    private val suffix = System.currentTimeMillis()
    private var testUser: User? = null
    private val createdStreakIds = mutableListOf<Long>()

    private val emptyData = buildJsonArray { add(0L) }
    private val seedData = buildJsonArray { add(0b11L) }

    @Before
    fun setup() = runBlocking {
        testUser = supabase.from("user")
            .insert(User(name = "streak_test_$suffix", mail = "streak_$suffix@test.com", age = 26)) { select() }
            .decodeSingle<User>()
    }

    @After
    fun cleanup() = runBlocking {
        createdStreakIds.forEach { id ->
            supabase.from("streak").delete { filter { eq("id", id) } }
        }
        createdStreakIds.clear()
        testUser?.id?.let { supabase.from("user").delete { filter { eq("id", it) } } }
        testUser = null
    }

    private suspend fun createTestStreak(): Streak {
        val created = supabase.from("streak")
            .insert(Streak(userId = testUser!!.id!!, data = seedData, startedAt = "2026-01-01T00:00:00Z")) { select() }
            .decodeSingle<Streak>()
        createdStreakIds += created.id!!
        return created
    }

    @Test
    fun `GET streaks returns 200 with list`() = testApp { client ->
        val response = client.get("/streaks")
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(response.body<List<Streak>>())
    }

    @Test
    fun `GET streaks filtered by user_id returns only that user's streaks`() = testApp { client ->
        val streak = createTestStreak()
        val response = client.get("/streaks?user_id=${testUser!!.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        val list = response.body<List<Streak>>()
        assertTrue(list.all { it.userId == testUser!!.id })
        assertTrue(list.any { it.id == streak.id })
    }

    @Test
    fun `POST streaks creates and returns 201`() = testApp { client ->
        val response = client.post("/streaks") {
            contentType(ContentType.Application.Json)
            setBody(Streak(userId = testUser!!.id!!, data = emptyData))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val streak = response.body<Streak>()
        assertNotNull(streak.id)
        assertEquals(testUser!!.id, streak.userId)
        createdStreakIds += streak.id!!
    }

    @Test
    fun `POST streaks with data and started_at creates correctly`() = testApp { client ->
        val response = client.post("/streaks") {
            contentType(ContentType.Application.Json)
            setBody(Streak(userId = testUser!!.id!!, data = seedData, startedAt = "2026-01-01T00:00:00Z"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val streak = response.body<Streak>()
        assertNotNull(streak.startedAt)
        assertEquals(1, streak.data.size)
        assertEquals(0b11L, streak.data[0].jsonPrimitive.long)
        createdStreakIds += streak.id!!
    }

    @Test
    fun `GET streaks by id returns 200`() = testApp { client ->
        val existing = createTestStreak()
        val response = client.get("/streaks/${existing.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(existing.id, response.body<Streak>().id)
    }

    @Test
    fun `GET streaks by invalid id returns 400`() = testApp { client ->
        val response = client.get("/streaks/abc")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET streaks by unknown id returns 404`() = testApp { client ->
        val response = client.get("/streaks/999999999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT streaks updates data and returns 200`() = testApp { client ->
        val existing = createTestStreak()
        val updatedData = buildJsonArray { add(0b111L) }
        val response = client.put("/streaks/${existing.id}") {
            contentType(ContentType.Application.Json)
            setBody(Streak(userId = testUser!!.id!!, data = updatedData))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(0b111L, response.body<Streak>().data[0].jsonPrimitive.long)
    }

    @Test
    fun `DELETE streaks returns 204`() = testApp { client ->
        val existing = createTestStreak()
        val response = client.delete("/streaks/${existing.id}")
        assertEquals(HttpStatusCode.NoContent, response.status)
        createdStreakIds.remove(existing.id!!)
    }
}
