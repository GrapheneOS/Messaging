package com.android.messaging.data.conversation.model.message

import com.android.messaging.datamodel.data.ConversationMessageData

/**
 * The [messages] of a conversation that [request] asked for, oldest first.
 *
 * [hasOlder] and [hasNewer] are proven, not inferred: the query asks for one row more than the
 * window on each side and that row is dropped here, so a full window on its own never implies more
 * messages exist. A window over the newest messages never has newer ones.
 *
 * [latestMessage] is the newest message of the conversation while it is past the window, so that a
 * message arriving there can still be told apart from the window moving.
 */
internal data class ConversationMessagesWindow(
    val request: ConversationMessagesWindowRequest,
    val messages: List<ConversationMessageData>,
    val hasOlder: Boolean,
    val hasNewer: Boolean = false,
    val latestMessage: ConversationMessageData? = null,
)
