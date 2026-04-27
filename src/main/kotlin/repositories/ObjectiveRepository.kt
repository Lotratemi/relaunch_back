package com.codingfactory.repositories

import com.codingfactory.Database
import com.codingfactory.Database.exec
import com.codingfactory.Database.fetchAll
import com.codingfactory.Database.fetchOne
import com.codingfactory.models.RelaunchModels.Objective
import io.github.jan.supabase.postgrest.from

class ObjectiveRepository {

    suspend fun findByUserId(userId: Long): List<Objective> = Database.run(
        prod = {
            it.from("objectives").select {
                filter { Objective::userId eq userId }
            }.decodeList()
        },
        test = {
            it.fetchAll("SELECT to_jsonb(o) FROM objectives o WHERE user_id = ?", userId)
        }
    )

    suspend fun findByIdAndUserId(id: Long, userId: Long): Objective? = Database.run(
        prod = {
            it.from("objectives").select {
                filter {
                    Objective::id eq id
                    Objective::userId eq userId
                }
            }.decodeSingleOrNull()
        },
        test = {
            it.fetchOne("SELECT to_jsonb(o) FROM objectives o WHERE id = ? AND user_id = ?", id, userId)
        }
    )

    suspend fun create(objective: Objective): Objective? = Database.run(
        prod = {
            it.from("objectives").insert(objective) { select() }.decodeSingleOrNull()
        },
        test = {
            it.fetchOne(
                """
                INSERT INTO objectives (user_id, end_at, frequency, title, description)
                VALUES (?, ?::timestamptz, ?, ?, ?)
                RETURNING to_jsonb(objectives.*)
                """.trimIndent(),
                objective.userId, objective.endAt, objective.frequency, objective.title, objective.description
            )
        }
    )

    suspend fun update(id: Long, userId: Long, objective: Objective): Objective? = Database.run(
        prod = {
            it.from("objectives").update(objective) {
                select()
                filter {
                    Objective::id eq id
                    Objective::userId eq userId
                }
            }.decodeSingleOrNull()
        },
        test = {
            it.fetchOne(
                """
                UPDATE objectives
                SET end_at = ?::timestamptz, frequency = ?, title = ?, description = ?
                WHERE id = ? AND user_id = ?
                RETURNING to_jsonb(objectives.*)
                """.trimIndent(),
                objective.endAt, objective.frequency, objective.title, objective.description, id, userId
            )
        }
    )

    suspend fun delete(id: Long, userId: Long): Unit = Database.run(
        prod = {
            it.from("objectives").delete {
                filter {
                    Objective::id eq id
                    Objective::userId eq userId
                }
            }
            Unit
        },
        test = {
            it.exec("DELETE FROM objectives WHERE id = ? AND user_id = ?", id, userId)
            Unit
        }
    )
}
