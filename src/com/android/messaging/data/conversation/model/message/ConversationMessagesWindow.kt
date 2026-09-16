package com.android.messaging.data.conversation.model.message

import com.android.messaging.datamodel.data.ConversationMessageData

internal data class ConversationMessagesWindow(
    val messages: List<ConversationMessageData>,
    val hasMore: Boolean,
)
