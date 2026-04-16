package com.codingfactory.models

import kotlinx.serialization.Serializable

class MistralModels {
    @Serializable
    data class CompletionArgs(
        val temperature: Double? = null,
        val max_tokens: Int? = null,
        val top_p: Double? = null
    )

    @Serializable
    data class ConversationCreateRequest(
        val inputs: String,
        val completion_args: CompletionArgs = CompletionArgs(),
        val model: String? = null,
        val store: Boolean = true,
        val stream: Boolean = false
    )

    @Serializable
    data class ConversationAppendRequest(
        val inputs: String,
        val completion_args: CompletionArgs = CompletionArgs(),
        val store: Boolean = true,
        val stream: Boolean = false
    )

    @Serializable
    data class OutputEntry(
        val type: String,
        val role: String? = null,
        val content: String? = null
    )

    @Serializable
    data class ConversationUsage(
        val prompt_tokens: Int,
        val completion_tokens: Int,
        val total_tokens: Int
    )

    @Serializable
    data class ConversationResponse(
        val conversation_id: String,
        val outputs: List<OutputEntry>,
        val usage: ConversationUsage,
        val `object`: String = "conversation.response"
    )
}
