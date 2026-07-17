package com.android.messaging.ui.conversation.screen

import com.android.messaging.data.conversation.model.MessageId
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationAutoScrollPolicyTest {

    @Test
    fun evaluateConversationAutoScroll_doesNotScrollWhenLatestMessageDidNotChange() {
        val decision = evaluateConversationAutoScroll(
            input = ConversationAutoScrollInput(
                previousLatestMessageId = MessageId("message-1"),
                latestMessageId = MessageId("message-1"),
                hasLatestMessage = true,
                isLatestMessageIncoming = false,
                wasScrolledToLatestMessage = true,
            ),
        )

        assertEquals(
            ConversationAutoScrollDecision(
                shouldScrollToLatestMessage = false,
                shouldShowNewMessageSnackbar = false,
                updatedLatestMessageId = MessageId("message-1"),
            ),
            decision,
        )
    }

    @Test
    fun evaluateConversationAutoScroll_doesNotScrollWhenThereIsNoLatestMessage() {
        val decision = evaluateConversationAutoScroll(
            input = ConversationAutoScrollInput(
                previousLatestMessageId = MessageId("message-1"),
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
                previousLatestMessageId = MessageId("message-1"),
                latestMessageId = MessageId("message-2"),
                hasLatestMessage = true,
                isLatestMessageIncoming = true,
                wasScrolledToLatestMessage = false,
            ),
        )

        assertEquals(
            ConversationAutoScrollDecision(
                shouldScrollToLatestMessage = false,
                shouldShowNewMessageSnackbar = true,
                updatedLatestMessageId = MessageId("message-2"),
            ),
            decision,
        )
    }

    @Test
    fun evaluateConversationAutoScroll_scrollsForIncomingMessageWhenUserIsAlreadyAtLatest() {
        val decision = evaluateConversationAutoScroll(
            input = ConversationAutoScrollInput(
                previousLatestMessageId = MessageId("message-1"),
                latestMessageId = MessageId("message-2"),
                hasLatestMessage = true,
                isLatestMessageIncoming = true,
                wasScrolledToLatestMessage = true,
            ),
        )

        assertEquals(
            ConversationAutoScrollDecision(
                shouldScrollToLatestMessage = true,
                shouldShowNewMessageSnackbar = false,
                updatedLatestMessageId = MessageId("message-2"),
            ),
            decision,
        )
    }

    @Test
    fun evaluateConversationAutoScroll_scrollsForOutgoingMessage() {
        val decision = evaluateConversationAutoScroll(
            input = ConversationAutoScrollInput(
                previousLatestMessageId = MessageId("message-1"),
                latestMessageId = MessageId("message-2"),
                hasLatestMessage = true,
                isLatestMessageIncoming = false,
                wasScrolledToLatestMessage = false,
            ),
        )

        assertEquals(
            ConversationAutoScrollDecision(
                shouldScrollToLatestMessage = true,
                shouldShowNewMessageSnackbar = false,
                updatedLatestMessageId = MessageId("message-2"),
            ),
            decision,
        )
    }
}
