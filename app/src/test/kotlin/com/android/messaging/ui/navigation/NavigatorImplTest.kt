package com.android.messaging.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.navigation.ConversationNavKey
import com.android.messaging.ui.conversation.navigation.NewChatNavKey
import com.android.messaging.ui.conversationsettings.navigation.ConversationSettingsNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorImplTest {

    private var didFinish = false

    @Test
    fun closeConversation_finishesWhenNothingRemains() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = CONVERSATION_ID),
            ConversationSettingsNavKey(conversationId = CONVERSATION_ID),
        )

        navigator(backStack = backStack).closeConversation(conversationId = CONVERSATION_ID)

        assertTrue(didFinish)
    }

    @Test
    fun closeConversation_dropsEveryTrailingDestinationOfThatConversation() {
        val backStack = mutableListOf(
            NewChatNavKey,
            ConversationNavKey(conversationId = CONVERSATION_ID),
            ConversationSettingsNavKey(conversationId = CONVERSATION_ID),
        )

        navigator(backStack = backStack).closeConversation(conversationId = CONVERSATION_ID)

        assertFalse(didFinish)
        assertEquals(listOf(NewChatNavKey), backStack)
    }

    @Test
    fun closeConversation_keepsDestinationsOfOtherConversations() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = ConversationId("other")),
            ConversationSettingsNavKey(conversationId = CONVERSATION_ID),
        )

        navigator(backStack = backStack).closeConversation(conversationId = CONVERSATION_ID)

        assertEquals(
            listOf(ConversationNavKey(conversationId = ConversationId("other"))),
            backStack,
        )
    }

    @Test
    fun back_finishesWhenBackStackHasSingleEntry() {
        val backStack = mutableListOf<NavKey>(NewChatNavKey)

        navigator(backStack = backStack).back()

        assertTrue(didFinish)
        assertEquals(listOf(NewChatNavKey), backStack)
    }

    @Test
    fun back_removesLastEntryWhenBackStackHasMultipleEntries() {
        val backStack = mutableListOf(
            NewChatNavKey,
            ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        navigator(backStack = backStack).back()

        assertFalse(didFinish)
        assertEquals(listOf(NewChatNavKey), backStack)
    }

    private fun navigator(backStack: MutableList<NavKey>): Navigator {
        return NavigatorImpl(
            backStack = backStack,
            navigationReducer = NavigationReducerImpl(),
            onFinish = { didFinish = true },
        )
    }
}
