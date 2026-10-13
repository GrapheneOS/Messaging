package com.android.messaging.data.search.model

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId

internal data class MessageSearchResult(
    val messageId: MessageId,
    val conversationId: ConversationId,
    val snippet: String,
    val timestamp: Long,
    val conversationName: String?,
    val senderName: String?,
)
