package com.codingfactory.repositories

import com.codingfactory.Database
import com.codingfactory.Database.fetchOne
import com.codingfactory.models.RelaunchModels.Conversation

class ConversationRepository {

    suspend fun findByUserIdAndMistralId(userId: Long, mistralConvId: String): Conversation? = Database.run {
        it.fetchOne(
            """SELECT to_jsonb(c) FROM conversation c WHERE user_id = ? AND mistral_conv_id = ?""",
            userId, mistralConvId
        )
    }

    suspend fun create(conversation: Conversation): Conversation = Database.run {
        it.fetchOne<Conversation>(
            """
            INSERT INTO conversation (user_id, mistral_conv_id) VALUES (?, ?)
            RETURNING to_jsonb(conversation.*)
            """.trimIndent(),
            conversation.userId, conversation.mistralConvId
        ) ?: error("Failed to insert conversation")
    }
}
