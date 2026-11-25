package com.android.messaging.data.conversationlist.model

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId

internal data class SearchableMessage(
    val messageId: MessageId,
    val conversationId: ConversationId,
    val text: String,
    val receivedTimestamp: Long,
    val isIncoming: Boolean,
    val senderName: String?,
)
