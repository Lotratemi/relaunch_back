package com.codingfactory.repositories

import com.codingfactory.Database
import com.codingfactory.Database.fetchOne
import com.codingfactory.models.RelaunchModels.Conversation
import io.github.jan.supabase.postgrest.from

class ConversationRepository {

    suspend fun findByUserIdAndMistralId(userId: Long, mistralConvId: String): Conversation? = Database.run(
        prod = {
            it.from("conversation").select {
                filter {
                    Conversation::userId eq userId
                    Conversation::mistralConvId eq mistralConvId
                }
            }.decodeSingleOrNull()
        },
        test = {
            it.fetchOne(
                """SELECT to_jsonb(c) FROM conversation c WHERE user_id = ? AND mistral_conv_id = ?""",
                userId, mistralConvId
            )
        }
    )

    suspend fun create(conversation: Conversation): Conversation = Database.run(
        prod = {
            it.from("conversation").insert(conversation) { select() }.decodeSingle()
        },
        test = {
            it.fetchOne<Conversation>(
                """
                INSERT INTO conversation (user_id, mistral_conv_id) VALUES (?, ?)
                RETURNING to_jsonb(conversation.*)
                """.trimIndent(),
                conversation.userId, conversation.mistralConvId
            ) ?: error("Failed to insert conversation")
        }
    )
}
