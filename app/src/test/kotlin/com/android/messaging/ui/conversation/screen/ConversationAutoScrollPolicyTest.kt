package com.android.messaging.ui.conversation.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationAutoScrollPolicyTest {

    @Test
    fun evaluateConversationAutoScroll_doesNotScrollWhenLatestMessageDidNotChange() {
        val decision = evaluateConversationAutoScroll(
            input = ConversationAutoScrollInput(
                previousLatestMessageId = "message-1",
                latestMessageId = "message-1",
                hasLatestMessage = true,
                isLatestMessageIncoming = false,
                wasScrolledToLatestMessage = true,
            ),
        )

        assertEquals(
            ConversationAutoScrollDecision(
                shouldScrollToLatestMessage = false,
                shouldShowNewMessageSnackbar = false,
                updatedLatestMessageId = "message-1",
            ),
            decision,
        )
    }

    @Test
    fun evaluateConversationAutoScroll_doesNotScrollWhenThereIsNoLatestMessage() {
        val decision = evaluateConversationAutoScroll(
            input = ConversationAutoScrollInput(
                previousLatestMessageId = "message-1",
                latestMessageId = null,
                hasLatestMessage = false,
                isLatestMessageIncoming = false,
                wasScrolledToLatestMessage = true,
            ),
        )

        assertEquals(
            ConversationAutoScrollDecision(
                shouldScrollToLatestMessage = false,
                shouldShowNewMessageSnackbar = false,
                updatedLatestMessageId = null,
            ),
            decision,
        )
    }

    @Test
    fun evaluateConversationAutoScroll_showsSnackbarForIncomingMessageWhenUserIsAwayFromLatest() {
        val decision = evaluateConversationAutoScroll(
            input = ConversationAutoScrollInput(
                previousLatestMessageId = "message-1",
                latestMessageId = "message-2",
                hasLatestMessage = true,
                isLatestMessageIncoming = true,
                wasScrolledToLatestMessage = false,
            ),
        )

        assertEquals(
            ConversationAutoScrollDecision(
                shouldScrollToLatestMessage = false,
                shouldShowNewMessageSnackbar = true,
                updatedLatestMessageId = "message-2",
            ),
            decision,
        )
    }

    @Test
    fun evaluateConversationAutoScroll_scrollsForIncomingMessageWhenUserIsAlreadyAtLatest() {
        val decision = evaluateConversationAutoScroll(
            input = ConversationAutoScrollInput(
                previousLatestMessageId = "message-1",
                latestMessageId = "message-2",
                hasLatestMessage = true,
                isLatestMessageIncoming = true,
                wasScrolledToLatestMessage = true,
            ),
        )

        assertEquals(
            ConversationAutoScrollDecision(
                shouldScrollToLatestMessage = true,
                shouldShowNewMessageSnackbar = false,
                updatedLatestMessageId = "message-2",
            ),
            decision,
        )
    }

    @Test
    fun evaluateConversationAutoScroll_scrollsForOutgoingMessage() {
        val decision = evaluateConversationAutoScroll(
            input = ConversationAutoScrollInput(
                previousLatestMessageId = "message-1",
                latestMessageId = "message-2",
                hasLatestMessage = true,
                isLatestMessageIncoming = false,
                wasScrolledToLatestMessage = false,
            ),
        )

        assertEquals(
            ConversationAutoScrollDecision(
                shouldScrollToLatestMessage = true,
                shouldShowNewMessageSnackbar = false,
                updatedLatestMessageId = "message-2",
            ),
            decision,
        )
    }
}
