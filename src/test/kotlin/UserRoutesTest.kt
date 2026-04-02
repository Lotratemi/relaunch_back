package com.codingfactory

import com.codingfactory.models.User
import io.github.jan.supabase.postgrest.from
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserRoutesTest {

    private val createdIds = mutableListOf<Long>()
    private val suffix = System.currentTimeMillis()

    @After
    fun cleanup() = runBlocking {
        createdIds.forEach { id ->
            supabase.from("user").delete { filter { eq("id", id) } }
        }
        createdIds.clear()
    }

    private suspend fun createTestUser(suffix: Long = this.suffix): User {
        val created = supabase.from("user")
            .insert(User(name = "test_$suffix", mail = "test_$suffix@test.com", age = 25)) { select() }
            .decodeSingle<User>()
        createdIds += created.id!!
        return created
    }

    @Test
    fun `GET users returns 200 with list`() = testApp { client ->
        val response = client.get("/users")
        assertEquals(HttpStatusCode.OK, response.status)
        val users = response.body<List<User>>()
        assertNotNull(users)
    }

    @Test
    fun `POST users creates and returns 201`() = testApp { client ->
        val response = client.post("/users") {
            contentType(ContentType.Application.Json)
            setBody(User(name = "new_$suffix", mail = "new_$suffix@test.com", age = 30))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val user = response.body<User>()
        assertNotNull(user.id)
        assertEquals("new_$suffix", user.name)
        assertEquals("new_$suffix@test.com", user.mail)
        assertEquals(30, user.age)
        createdIds += user.id!!
    }

    @Test
    fun `GET users by id returns 200`() = testApp { client ->
        val existing = createTestUser()
        val response = client.get("/users/${existing.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        val user = response.body<User>()
        assertEquals(existing.id, user.id)
        assertEquals(existing.name, user.name)
    }

    @Test
    fun `GET users by invalid id returns 400`() = testApp { client ->
        val response = client.get("/users/not-a-number")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET users by unknown id returns 404`() = testApp { client ->
        val response = client.get("/users/999999999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT users updates and returns 200`() = testApp { client ->
        val existing = createTestUser()
        val response = client.put("/users/${existing.id}") {
            contentType(ContentType.Application.Json)
            setBody(User(name = "updated_$suffix", mail = "updated_$suffix@test.com", age = 99))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val user = response.body<User>()
        assertEquals("updated_$suffix", user.name)
        assertEquals(99, user.age)
    }

    @Test
    fun `DELETE users returns 204`() = testApp { client ->
        val existing = createTestUser()
        val response = client.delete("/users/${existing.id}")
        assertEquals(HttpStatusCode.NoContent, response.status)
        createdIds.remove(existing.id!!)
    }

    @Test
    fun `GET users returns all created users`() = testApp { client ->
        val s1 = suffix
        val s2 = suffix + 1
        val u1 = createTestUser(s1)
        val u2 = createTestUser(s2)
        val response = client.get("/users")
        assertEquals(HttpStatusCode.OK, response.status)
        val users = response.body<List<User>>()
        assertTrue(users.any { it.id == u1.id })
        assertTrue(users.any { it.id == u2.id })
    }
}
