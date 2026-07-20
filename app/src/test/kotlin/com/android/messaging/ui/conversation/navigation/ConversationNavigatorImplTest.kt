package com.android.messaging.ui.conversation.navigation

import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.assertThat
import com.android.messaging.ui.navigation.NavigationReducerImpl
import com.android.messaging.ui.navigation.NavigatorImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationNavigatorImplTest {

    private var didFinish = false

    @Test
    fun navigateToConversation_replacesNewChatEntryFlowWithConversation() {
        val backStack = mutableListOf<NavKey>(NewChatNavKey)

        navigator(backStack = backStack).navigateToConversation(conversationId = CONVERSATION_ID)

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = CONVERSATION_ID),
            ),
            backStack,
        )
    }

    @Test
    fun navigateToConversation_removesRecipientPickerEntryFlowBeforeNavigating() {
        val backStack = mutableListOf(
            NewChatNavKey,
            RecipientPickerNavKey(mode = RecipientPickerMode.CREATE_GROUP),
        )

        navigator(backStack = backStack).navigateToConversation(conversationId = CONVERSATION_ID)

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = CONVERSATION_ID),
            ),
            backStack,
        )
    }

    @Test
    fun navigateToConversation_appendsWhenAlreadyInsideConversationFlow() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        navigator(backStack = backStack).navigateToConversation(
            conversationId = ConversationId("conversation-2"),
        )

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = CONVERSATION_ID),
                ConversationNavKey(conversationId = ConversationId("conversation-2")),
            ),
            backStack,
        )
    }

    @Test
    fun navigateToRecipientPicker_doesNotDuplicateExistingTopDestination() {
        val backStack = mutableListOf(
            NewChatNavKey,
            RecipientPickerNavKey(mode = RecipientPickerMode.ADD_PARTICIPANTS),
        )

        navigator(backStack = backStack).navigateToRecipientPicker(
            mode = RecipientPickerMode.ADD_PARTICIPANTS,
        )

        assertEquals(
            listOf(
                NewChatNavKey,
                RecipientPickerNavKey(mode = RecipientPickerMode.ADD_PARTICIPANTS),
            ),
            backStack,
        )
    }

    @Test
    fun navigateToAddParticipants_appendsDestinationWhenItIsNotAlreadyOnTop() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        navigator(backStack = backStack).navigateToAddParticipants(
            conversationId = CONVERSATION_ID,
        )

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = CONVERSATION_ID),
                AddParticipantsNavKey(conversationId = CONVERSATION_ID),
            ),
            backStack,
        )
    }

    @Test
    fun navigateToAddParticipants_doesNotDuplicateExistingTopDestination() {
        val backStack = mutableListOf(
            ConversationNavKey(conversationId = CONVERSATION_ID),
            AddParticipantsNavKey(conversationId = CONVERSATION_ID),
        )

        navigator(backStack = backStack).navigateToAddParticipants(
            conversationId = CONVERSATION_ID,
        )

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = CONVERSATION_ID),
                AddParticipantsNavKey(conversationId = CONVERSATION_ID),
            ),
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
        assertEquals(
            listOf(NewChatNavKey),
            backStack,
        )
    }

    @Test
    fun replaceCurrentConversation_removesAddParticipantsAndReplacesExistingConversation() {
        val backStack = mutableListOf(
            ConversationNavKey(conversationId = CONVERSATION_ID),
            AddParticipantsNavKey(conversationId = CONVERSATION_ID),
        )

        navigator(backStack = backStack).replaceCurrentConversation(
            conversationId = ConversationId("conversation-2"),
        )

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = ConversationId("conversation-2")),
            ),
            backStack,
        )
    }

    @Test
    fun replaceCurrentConversation_addsConversationWhenBackStackHasNoConversationEntry() {
        val backStack = mutableListOf(
            NewChatNavKey,
            AddParticipantsNavKey(conversationId = CONVERSATION_ID),
        )

        navigator(backStack = backStack).replaceCurrentConversation(
            conversationId = ConversationId("conversation-2"),
        )

        assertEquals(
            listOf(
                NewChatNavKey,
                ConversationNavKey(conversationId = ConversationId("conversation-2")),
            ),
            backStack,
        )
    }

    @Test
    fun navigateToNewChat_pushesNewChatDestination() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        navigator(backStack = backStack).navigateToNewChat()

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = CONVERSATION_ID),
                NewChatNavKey,
            ),
            backStack,
        )
    }

    @Test
    fun navigateToMessageDetails_appendsMessageDetailsDestination() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = ConversationId("c")),
        )

        navigator(backStack = backStack).navigateToMessageDetails(
            conversationId = ConversationId("c"),
            messageId = MessageId("m"),
        )

        assertThat(backStack.last()).isEqualTo(
            MessageDetailsNavKey(
                conversationId = ConversationId("c"),
                messageId = MessageId("m"),
            )
        )
        assertEquals(2, backStack.size)
    }

    @Test
    fun navigateToMessageDetails_whenAlreadyOnTop_doesNotDuplicate() {
        val backStack = mutableListOf(
            ConversationNavKey(conversationId = ConversationId("c")),
            MessageDetailsNavKey(
                conversationId = ConversationId("c"),
                messageId = MessageId("m"),
            ),
        )

        navigator(backStack = backStack).navigateToMessageDetails(
            conversationId = ConversationId("c"),
            messageId = MessageId("m"),
        )

        assertEquals(2, backStack.size)
    }

    private fun navigator(backStack: MutableList<NavKey>): ConversationNavigator {
        return ConversationNavigatorImpl(
            navigator = NavigatorImpl(
                backStack = backStack,
                navigationReducer = NavigationReducerImpl(),
                onFinish = { didFinish = true },
            ),
        )
    }
}
