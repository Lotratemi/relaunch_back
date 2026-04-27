package com.codingfactory.services

import com.codingfactory.models.RelaunchModels.Objective
import com.codingfactory.repositories.ObjectiveRepository

class ObjectiveService(private val repo: ObjectiveRepository) {

    suspend fun getAllForUser(userId: Long): List<Objective> = repo.findByUserId(userId)

    suspend fun getByIdForUser(id: Long, userId: Long): Objective? = repo.findByIdAndUserId(id, userId)

    suspend fun create(objective: Objective): Objective? = repo.create(objective)

    suspend fun update(id: Long, userId: Long, objective: Objective): Objective? = repo.update(id, userId, objective)

    suspend fun delete(id: Long, userId: Long) = repo.delete(id, userId)
}
