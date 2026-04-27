package com.codingfactory.services

import com.codingfactory.models.RelaunchModels.Streak
import com.codingfactory.repositories.StreakRepository

class StreakService(private val repo: StreakRepository) {

    suspend fun getAllForUser(userId: Long): List<Streak> = repo.findByUserId(userId)

    suspend fun getByIdForUser(id: Long, userId: Long): Streak? = repo.findByIdAndUserId(id, userId)

    suspend fun create(streak: Streak): Streak? = repo.create(streak)

    suspend fun update(id: Long, userId: Long, streak: Streak): Streak? = repo.update(id, userId, streak)

    suspend fun delete(id: Long, userId: Long) = repo.delete(id, userId)
}
