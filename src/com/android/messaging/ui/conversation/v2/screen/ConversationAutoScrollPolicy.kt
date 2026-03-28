package com.android.messaging.ui.conversation.v2.screen

internal data class ConversationAutoScrollInput(
    val previousLatestMessageId: String?,
    val latestMessageId: String?,
    val hasLatestMessage: Boolean,
    val isLatestMessageIncoming: Boolean,
    val wasScrolledToLatestMessage: Boolean,
)

internal data class ConversationAutoScrollDecision(
    val shouldScrollToLatestMessage: Boolean,
    val updatedLatestMessageId: String?,
)

internal fun evaluateConversationAutoScroll(
    input: ConversationAutoScrollInput,
): ConversationAutoScrollDecision {
    return when {
        input.latestMessageId == input.previousLatestMessageId -> ConversationAutoScrollDecision(
            shouldScrollToLatestMessage = false,
            updatedLatestMessageId = input.latestMessageId,
        )

        !input.hasLatestMessage -> ConversationAutoScrollDecision(
            shouldScrollToLatestMessage = false,
            updatedLatestMessageId = input.latestMessageId,
        )

        input.isLatestMessageIncoming && !input.wasScrolledToLatestMessage -> {
            ConversationAutoScrollDecision(
                shouldScrollToLatestMessage = false,
                updatedLatestMessageId = input.latestMessageId,
            )
        }

        else -> ConversationAutoScrollDecision(
            shouldScrollToLatestMessage = true,
            updatedLatestMessageId = input.latestMessageId,
        )
    }
}
