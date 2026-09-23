package com.android.messaging.data.conversation.model.message

import com.android.messaging.datamodel.data.ConversationMessageData

/**
 * The newest [messages] of a conversation, oldest first.
 *
 * [hasMore] is proven, not inferred: the query asks for one row more than the window and that
 * row is dropped here, so a full window on its own never implies older messages exist.
 */
internal data class ConversationMessagesWindow(
    val messages: List<ConversationMessageData>,
    val hasMore: Boolean,
)
