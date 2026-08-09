package com.android.messaging.data.conversation.model

@JvmInline
internal value class ParticipantId(
    val value: String,
) {
    companion object {
        fun fromOrNull(value: String?): ParticipantId? {
            return value?.takeIf { it.isNotBlank() }?.let(::ParticipantId)
        }
    }
}
