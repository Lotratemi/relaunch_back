package com.codingfactory.repositories

import com.codingfactory.Database
import com.codingfactory.models.RelaunchModels.Conversation
import io.github.jan.supabase.postgrest.from

class ConversationRepository {

    private val db get() = Database.supabase

    suspend fun findByUserIdAndMistralId(userId: Long, mistralConvId: String): Conversation? =
        db.from("conversation").select {
            filter {
                Conversation::userId eq userId
                Conversation::mistralConvId eq mistralConvId
            }
        }.decodeSingleOrNull()

    suspend fun create(conversation: Conversation): Conversation =
        db.from("conversation").insert(conversation) { select() }.decodeSingle()
}
