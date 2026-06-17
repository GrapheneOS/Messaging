package com.android.messaging.domain.shareintent.model

import com.android.messaging.data.conversation.model.draft.ConversationDraft

internal data class SharedConversationDraftResult(
    val draft: ConversationDraft?,
    val hasDroppedContent: Boolean,
)
