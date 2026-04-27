package com.codingfactory.repositories

import com.codingfactory.Database
import com.codingfactory.models.RelaunchModels.Streak
import io.github.jan.supabase.postgrest.from

class StreakRepository {

    private val db get() = Database.supabase

    suspend fun findByUserId(userId: Long): List<Streak> =
        db.from("streak")
            .select { filter { Streak::userId eq userId } }
            .decodeList()

    suspend fun findByIdAndUserId(id: Long, userId: Long): Streak? =
        db.from("streak")
            .select {
                filter {
                    Streak::id eq id
                    Streak::userId eq userId
                }
            }.decodeSingleOrNull()

    suspend fun create(streak: Streak): Streak? =
        db.from("streak").insert(streak) { select() }.decodeSingleOrNull()

    suspend fun update(id: Long, userId: Long, streak: Streak): Streak? =
        db.from("streak").update(streak) {
            select()
            filter {
                Streak::id eq id
                Streak::userId eq userId
            }
        }.decodeSingleOrNull()

    suspend fun delete(id: Long, userId: Long) {
        db.from("streak").delete {
            filter {
                Streak::id eq id
                Streak::userId eq userId
            }
        }
    }
}
