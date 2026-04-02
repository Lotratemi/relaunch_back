package com.codingfactory

import com.codingfactory.models.Objective
import com.codingfactory.models.User
import io.github.jan.supabase.postgrest.from
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ObjectiveRoutesTest {

    private val suffix = System.currentTimeMillis()
    private var testUser: User? = null
    private val createdObjectiveIds = mutableListOf<Long>()

    @Before
    fun setup() = runBlocking {
        testUser = supabase.from("user")
            .insert(User(name = "obj_test_$suffix", mail = "obj_$suffix@test.com", age = 28)) { select() }
            .decodeSingle<User>()
    }

    @After
    fun cleanup() = runBlocking {
        createdObjectiveIds.forEach { id ->
            supabase.from("objectives").delete { filter { eq("id", id) } }
        }
        createdObjectiveIds.clear()
        testUser?.id?.let { supabase.from("user").delete { filter { eq("id", it) } } }
        testUser = null
    }

    private val baseObjective get() = Objective(
        userId = testUser!!.id,
        endAt = "2026-12-31T00:00:00Z",
        frequency = 7,
        title = "Test objective $suffix"
    )

    private suspend fun createTestObjective(): Objective {
        val created = supabase.from("objectives")
            .insert(baseObjective) { select() }
            .decodeSingle<Objective>()
        createdObjectiveIds += created.id!!
        return created
    }

    @Test
    fun `GET objectives returns 200 with list`() = testApp { client ->
        val response = client.get("/objectives")
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(response.body<List<Objective>>())
    }

    @Test
    fun `GET objectives filtered by user_id returns only that user's objectives`() = testApp { client ->
        val obj = createTestObjective()
        val response = client.get("/objectives?user_id=${testUser!!.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        val list = response.body<List<Objective>>()
        assertTrue(list.all { it.userId == testUser!!.id })
        assertTrue(list.any { it.id == obj.id })
    }

    @Test
    fun `POST objectives creates and returns 201`() = testApp { client ->
        val response = client.post("/objectives") {
            contentType(ContentType.Application.Json)
            setBody(baseObjective)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val obj = response.body<Objective>()
        assertNotNull(obj.id)
        assertEquals(testUser!!.id, obj.userId)
        assertEquals("Test objective $suffix", obj.title)
        assertEquals(7L, obj.frequency)
        createdObjectiveIds += obj.id!!
    }

    @Test
    fun `POST objectives with description creates correctly`() = testApp { client ->
        val response = client.post("/objectives") {
            contentType(ContentType.Application.Json)
            setBody(baseObjective.copy(description = "Some details"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val obj = response.body<Objective>()
        assertEquals("Some details", obj.description)
        createdObjectiveIds += obj.id!!
    }

    @Test
    fun `GET objectives by id returns 200`() = testApp { client ->
        val existing = createTestObjective()
        val response = client.get("/objectives/${existing.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(existing.id, response.body<Objective>().id)
    }

    @Test
    fun `GET objectives by invalid id returns 400`() = testApp { client ->
        val response = client.get("/objectives/abc")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET objectives by unknown id returns 404`() = testApp { client ->
        val response = client.get("/objectives/999999999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT objectives updates and returns 200`() = testApp { client ->
        val existing = createTestObjective()
        val response = client.put("/objectives/${existing.id}") {
            contentType(ContentType.Application.Json)
            setBody(baseObjective.copy(title = "Updated title", frequency = 14))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val obj = response.body<Objective>()
        assertEquals("Updated title", obj.title)
        assertEquals(14L, obj.frequency)
    }

    @Test
    fun `DELETE objectives returns 204`() = testApp { client ->
        val existing = createTestObjective()
        val response = client.delete("/objectives/${existing.id}")
        assertEquals(HttpStatusCode.NoContent, response.status)
        createdObjectiveIds.remove(existing.id!!)
    }
}
