package com.android.messaging.data.conversation.model.message

import com.android.messaging.data.conversation.model.MessageId

/** Which messages of a conversation a window holds. */
internal sealed interface ConversationMessagesWindowRequest {

    /** The newest [size] messages. */
    data class Newest(
        val size: Int,
    ) : ConversationMessagesWindowRequest

    /**
     * Up to [olderSize] messages up to and including the one at [position], and up to [newerSize]
     * after it, so that a message far from the newest one opens without loading everything newer.
     */
    data class Around(
        val position: ConversationMessagePosition,
        val olderSize: Int,
        val newerSize: Int,
    ) : ConversationMessagesWindowRequest
}

/**
 * Where a message sorts in its conversation: by received timestamp, then by id, the order the
 * timestamp index keeps them in. A window around it stays put even once the message is deleted.
 */
internal data class ConversationMessagePosition(
    val messageId: MessageId,
    val receivedTimestamp: Long,
)
