package com.codingfactory

import com.codingfactory.models.Conversation
import com.codingfactory.models.Message
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

class MessageRoutesTest {

    private val suffix = System.currentTimeMillis()
    private var testUser: User? = null
    private var testConversation: Conversation? = null
    private val createdMessageIds = mutableListOf<Long>()

    @Before
    fun setup() = runBlocking {
        testUser = supabase.from("user")
            .insert(User(name = "msg_test_$suffix", mail = "msg_$suffix@test.com", age = 22)) { select() }
            .decodeSingle<User>()
        testConversation = supabase.from("conversation")
            .insert(Conversation(userId = testUser!!.id!!)) { select() }
            .decodeSingle<Conversation>()
    }

    @After
    fun cleanup() = runBlocking {
        createdMessageIds.forEach { id ->
            supabase.from("message").delete { filter { eq("id", id) } }
        }
        createdMessageIds.clear()
        testConversation?.id?.let { supabase.from("conversation").delete { filter { eq("id", it) } } }
        testUser?.id?.let { supabase.from("user").delete { filter { eq("id", it) } } }
        testConversation = null
        testUser = null
    }

    private suspend fun createTestMessage(isUser: Boolean = true, content: String = "hello"): Message {
        val created = supabase.from("message")
            .insert(Message(conversationId = testConversation!!.id!!, isUser = isUser, content = content)) { select() }
            .decodeSingle<Message>()
        createdMessageIds += created.id!!
        return created
    }

    @Test
    fun `GET messages returns 200 with list`() = testApp { client ->
        val response = client.get("/messages")
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(response.body<List<Message>>())
    }

    @Test
    fun `GET messages filtered by conversation_id returns only that conversation's messages`() = testApp { client ->
        val m = createTestMessage(content = "filtered message")
        val response = client.get("/messages?conversation_id=${testConversation!!.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        val list = response.body<List<Message>>()
        assertTrue(list.all { it.conversationId == testConversation!!.id })
        assertTrue(list.any { it.id == m.id })
    }

    @Test
    fun `POST messages creates and returns 201`() = testApp { client ->
        val response = client.post("/messages") {
            contentType(ContentType.Application.Json)
            setBody(Message(conversationId = testConversation!!.id!!, isUser = true, content = "test content"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val message = response.body<Message>()
        assertNotNull(message.id)
        assertEquals(testConversation!!.id, message.conversationId)
        assertEquals("test content", message.content)
        assertTrue(message.isUser)
        createdMessageIds += message.id!!
    }

    @Test
    fun `POST messages creates bot message`() = testApp { client ->
        val response = client.post("/messages") {
            contentType(ContentType.Application.Json)
            setBody(Message(conversationId = testConversation!!.id!!, isUser = false, content = "bot reply"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val message = response.body<Message>()
        assertEquals(false, message.isUser)
        assertEquals("bot reply", message.content)
        createdMessageIds += message.id!!
    }

    @Test
    fun `GET messages by id returns 200`() = testApp { client ->
        val existing = createTestMessage()
        val response = client.get("/messages/${existing.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(existing.id, response.body<Message>().id)
    }

    @Test
    fun `GET messages by invalid id returns 400`() = testApp { client ->
        val response = client.get("/messages/abc")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET messages by unknown id returns 404`() = testApp { client ->
        val response = client.get("/messages/999999999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE messages returns 204`() = testApp { client ->
        val existing = createTestMessage()
        val response = client.delete("/messages/${existing.id}")
        assertEquals(HttpStatusCode.NoContent, response.status)
        createdMessageIds.remove(existing.id!!)
    }
}
