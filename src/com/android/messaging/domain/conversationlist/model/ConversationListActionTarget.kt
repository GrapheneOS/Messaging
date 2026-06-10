package com.android.messaging.domain.conversationlist.model

internal data class ConversationListActionTarget(
    val conversationId: String,
    val cutoffTimestampMillis: Long,
)
