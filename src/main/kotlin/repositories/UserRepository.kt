package com.codingfactory.repositories

import com.codingfactory.Database
import com.codingfactory.Database.exec
import com.codingfactory.Database.fetchOne
import com.codingfactory.models.RelaunchModels.User
import io.github.jan.supabase.postgrest.from

class UserRepository {

    suspend fun findById(id: Long): User? = Database.run(
        prod = {
            it.from("user").select { filter { User::id eq id } }.decodeSingleOrNull()
        },
        test = {
            it.fetchOne("""SELECT to_jsonb(u) FROM "user" u WHERE id = ?""", id)
        }
    )

    suspend fun create(user: User): User = Database.run(
        prod = {
            it.from("user").insert(user) { select() }.decodeSingle()
        },
        test = {
            it.fetchOne<User>(
                """
                INSERT INTO "user" (name, mail, age) VALUES (?, ?, ?)
                RETURNING to_jsonb("user".*)
                """.trimIndent(),
                user.name, user.mail, user.age.toInt()
            ) ?: error("Failed to insert user")
        }
    )

    suspend fun update(id: Long, user: User): User? = Database.run(
        prod = {
            it.from("user").update(user) {
                select()
                filter { User::id eq id }
            }.decodeSingleOrNull()
        },
        test = {
            it.fetchOne(
                """
                UPDATE "user" SET name = ?, mail = ?, age = ?
                WHERE id = ? RETURNING to_jsonb("user".*)
                """.trimIndent(),
                user.name, user.mail, user.age.toInt(), id
            )
        }
    )

    suspend fun delete(id: Long): Unit = Database.run(
        prod = {
            it.from("user").delete { filter { User::id eq id } }
            Unit
        },
        test = {
            it.exec("""DELETE FROM "user" WHERE id = ?""", id)
            Unit
        }
    )
}
