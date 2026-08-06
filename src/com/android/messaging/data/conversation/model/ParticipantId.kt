package com.android.messaging.data.conversation.model

@JvmInline
internal value class ParticipantId(
    val value: String,
) {
    fun isBlank(): Boolean {
        return value.isBlank()
    }

    fun isNotBlank(): Boolean {
        return value.isNotBlank()
    }

    companion object {
        fun fromOrNull(value: String?): ParticipantId? {
            return value?.let(::ParticipantId)
        }
    }
}
