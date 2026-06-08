package com.android.messaging.ui.conversation.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ConversationNavigationReducerImplTest {

    private val reducer = ConversationNavigationReducerImpl()

    @Test
    fun navigateToMessageDetails_appendsMessageDetailsDestination() {
        val backStack = mutableListOf<NavKey>(ConversationNavKey(conversationId = "c"))

        reducer.navigateToMessageDetails(
            backStack = backStack,
            conversationId = "c",
            messageId = "m",
        )

        assertEquals(
            MessageDetailsNavKey(
                conversationId = "c",
                messageId = "m",
            ),
            backStack.last(),
        )
        assertEquals(2, backStack.size)
    }

    @Test
    fun navigateToMessageDetails_whenAlreadyOnTop_doesNotDuplicate() {
        val backStack = mutableListOf(
            ConversationNavKey(conversationId = "c"),
            MessageDetailsNavKey(
                conversationId = "c",
                messageId = "m",
            ),
        )

        reducer.navigateToMessageDetails(
            backStack = backStack,
            conversationId = "c",
            messageId = "m",
        )

        assertEquals(2, backStack.size)
    }
}
