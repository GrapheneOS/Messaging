package com.android.messaging.data.conversation.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ArchivedConversation(
    val conversationId: ConversationId,
    val wasPinned: Boolean,
)
