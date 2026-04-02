package com.codingfactory

import com.codingfactory.models.Conversation
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

class ConversationRoutesTest {

    private val suffix = System.currentTimeMillis()
    private var testUser: User? = null
    private val createdConversationIds = mutableListOf<Long>()

    @Before
    fun setup() = runBlocking {
        testUser = supabase.from("user")
            .insert(User(name = "conv_test_$suffix", mail = "conv_$suffix@test.com", age = 20)) { select() }
            .decodeSingle<User>()
    }

    @After
    fun cleanup() = runBlocking {
        createdConversationIds.forEach { id ->
            supabase.from("conversation").delete { filter { eq("id", id) } }
        }
        createdConversationIds.clear()
        testUser?.id?.let { supabase.from("user").delete { filter { eq("id", it) } } }
        testUser = null
    }

    private suspend fun createTestConversation(): Conversation {
        val created = supabase.from("conversation")
            .insert(Conversation(userId = testUser!!.id!!)) { select() }
            .decodeSingle<Conversation>()
        createdConversationIds += created.id!!
        return created
    }

    @Test
    fun `GET conversations returns 200 with list`() = testApp { client ->
        val response = client.get("/conversations")
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(response.body<List<Conversation>>())
    }

    @Test
    fun `GET conversations filtered by user_id returns only that user's conversations`() = testApp { client ->
        val c = createTestConversation()
        val response = client.get("/conversations?user_id=${testUser!!.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        val list = response.body<List<Conversation>>()
        assertTrue(list.all { it.userId == testUser!!.id })
        assertTrue(list.any { it.id == c.id })
    }

    @Test
    fun `POST conversations creates and returns 201`() = testApp { client ->
        val response = client.post("/conversations") {
            contentType(ContentType.Application.Json)
            setBody(Conversation(userId = testUser!!.id!!))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val conv = response.body<Conversation>()
        assertNotNull(conv.id)
        assertEquals(testUser!!.id, conv.userId)
        createdConversationIds += conv.id!!
    }

    @Test
    fun `GET conversations by id returns 200`() = testApp { client ->
        val existing = createTestConversation()
        val response = client.get("/conversations/${existing.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(existing.id, response.body<Conversation>().id)
    }

    @Test
    fun `GET conversations by invalid id returns 400`() = testApp { client ->
        val response = client.get("/conversations/abc")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET conversations by unknown id returns 404`() = testApp { client ->
        val response = client.get("/conversations/999999999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT conversations updates objective_set`() = testApp { client ->
        val existing = createTestConversation()
        val response = client.put("/conversations/${existing.id}") {
            contentType(ContentType.Application.Json)
            setBody(Conversation(userId = testUser!!.id!!, objectiveSet = true))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(true, response.body<Conversation>().objectiveSet)
    }

    @Test
    fun `DELETE conversations returns 204`() = testApp { client ->
        val existing = createTestConversation()
        val response = client.delete("/conversations/${existing.id}")
        assertEquals(HttpStatusCode.NoContent, response.status)
        createdConversationIds.remove(existing.id!!)
    }
}
