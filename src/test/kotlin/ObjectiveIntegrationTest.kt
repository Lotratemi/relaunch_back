package com.codingfactory

import com.codingfactory.models.RelaunchModels.Objective
import com.codingfactory.models.RelaunchModels.User
import com.codingfactory.repositories.ObjectiveRepository
import com.codingfactory.repositories.UserRepository
import com.codingfactory.services.ObjectiveService
import com.codingfactory.services.UserService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ObjectiveIntegrationTest {

    private val users = UserService(UserRepository())
    private val objectives = ObjectiveService(ObjectiveRepository())

    private lateinit var testUser: User

    @BeforeEach
    fun setup() = runBlocking {
        Database.connection().use {
            it.createStatement().execute("""TRUNCATE TABLE objectives, "user" RESTART IDENTITY CASCADE""")
        }
        testUser = users.create(User(name = "test", mail = "test@example.com", age = 25))
    }

    private fun newObjective(title: String = "Run 5km", frequency: Long = 7, description: String? = null) =
        Objective(
            userId = testUser.id,
            endAt = "2026-12-31T00:00:00Z",
            frequency = frequency,
            title = title,
            description = description
        )

    @Test
    fun `create objective persists all fields`() = runBlocking {
        val created = objectives.create(newObjective(description = "Cardio"))
        assertNotNull(created)
        assertEquals(testUser.id, created!!.userId)
        assertEquals("Run 5km", created.title)
        assertEquals(7L, created.frequency)
        assertEquals("Cardio", created.description)
        assertNotNull(created.id)
    }

    @Test
    fun `create objective persists is_completed flag`() = runBlocking {
        val created = objectives.create(newObjective(title = "Done").copy(isComplet = true))
        assertNotNull(created)
        assertTrue(created!!.isComplet)

        val reloaded = objectives.getByIdForUser(created.id!!, testUser.id!!)
        assertNotNull(reloaded)
        assertTrue(reloaded!!.isComplet)
    }

    @Test
    fun `create objective defaults is_completed to false`() = runBlocking {
        val created = objectives.create(newObjective(title = "Todo"))!!
        assertEquals(false, created.isComplet)
    }

    @Test
    fun `update objective toggles is_completed`() = runBlocking {
        val created = objectives.create(newObjective(title = "Toggle"))!!
        val updated = objectives.update(
            created.id!!, testUser.id!!,
            newObjective(title = "Toggle").copy(isComplet = true)
        )
        assertNotNull(updated)
        assertTrue(updated!!.isComplet)
    }

    @Test
    fun `list objectives returns only the target user's objectives`() = runBlocking {
        objectives.create(newObjective(title = "A"))
        objectives.create(newObjective(title = "B"))
        val other = users.create(User(name = "other", mail = "other@example.com", age = 30))
        objectives.create(newObjective(title = "C").copy(userId = other.id))

        val result = objectives.getAllForUser(testUser.id!!)

        assertEquals(2, result.size)
        assertTrue(result.all { it.userId == testUser.id })
    }

    @Test
    fun `get objective by id returns matching record`() = runBlocking {
        val created = objectives.create(newObjective(title = "Specific"))!!
        val found = objectives.getByIdForUser(created.id!!, testUser.id!!)
        assertNotNull(found)
        assertEquals("Specific", found!!.title)
    }

    @Test
    fun `update objective changes specified fields`() = runBlocking {
        val created = objectives.create(newObjective(title = "Old"))!!
        val updated = objectives.update(
            created.id!!, testUser.id!!,
            newObjective(title = "New", frequency = 14)
        )
        assertNotNull(updated)
        assertEquals("New", updated!!.title)
        assertEquals(14L, updated.frequency)
    }

    @Test
    fun `delete objective removes it`() = runBlocking {
        val created = objectives.create(newObjective())!!
        objectives.delete(created.id!!, testUser.id!!)
        assertNull(objectives.getByIdForUser(created.id!!, testUser.id!!))
    }
}
