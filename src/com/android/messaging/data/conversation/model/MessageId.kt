package com.android.messaging.data.conversation.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
internal value class MessageId(
    val value: String,
) {
    fun isBlank(): Boolean {
        return value.isBlank()
    }

    fun isNotBlank(): Boolean {
        return value.isNotBlank()
    }

    companion object {
        fun fromOrNull(value: String?): MessageId? {
            return value?.let(::MessageId)
        }
    }
}
