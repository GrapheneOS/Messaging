package com.android.messaging.data.conversation.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
internal value class ConversationId(
    val value: String,
) {
    fun isBlank(): Boolean {
        return value.isBlank()
    }

    fun isNotBlank(): Boolean {
        return value.isNotBlank()
    }

    companion object {
        fun fromOrNull(value: String?): ConversationId? {
            return value?.let(::ConversationId)
        }
    }
}
