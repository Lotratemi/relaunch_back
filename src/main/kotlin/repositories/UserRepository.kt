package com.codingfactory.repositories

import com.codingfactory.Database
import com.codingfactory.Database.exec
import com.codingfactory.Database.fetchOne
import com.codingfactory.models.RelaunchModels.User

class UserRepository {

    suspend fun findById(id: Long): User? = Database.run {
        it.fetchOne("""SELECT to_jsonb(u) FROM "user" u WHERE id = ?""", id)
    }

    suspend fun create(user: User): User = Database.run {
        it.fetchOne<User>(
            """
            INSERT INTO "user" (name, mail, age) VALUES (?, ?, ?)
            RETURNING to_jsonb("user".*)
            """.trimIndent(),
            user.name, user.mail, user.age.toInt()
        ) ?: error("Failed to insert user")
    }

    suspend fun update(id: Long, user: User): User? = Database.run {
        it.fetchOne(
            """
            UPDATE "user" SET name = ?, mail = ?, age = ?
            WHERE id = ? RETURNING to_jsonb("user".*)
            """.trimIndent(),
            user.name, user.mail, user.age.toInt(), id
        )
    }

    suspend fun delete(id: Long): Unit = Database.run {
        it.exec("""DELETE FROM "user" WHERE id = ?""", id)
        Unit
    }
}
