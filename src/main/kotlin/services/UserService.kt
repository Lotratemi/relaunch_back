package com.codingfactory.services

import com.codingfactory.models.RelaunchModels.User
import com.codingfactory.repositories.UserRepository

class UserService(private val repo: UserRepository) {

    suspend fun getById(id: Long): User? = repo.findById(id)

    suspend fun create(user: User): User = repo.create(user)

    suspend fun update(id: Long, user: User): User? = repo.update(id, user)

    suspend fun delete(id: Long) = repo.delete(id)
}
