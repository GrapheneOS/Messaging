package com.android.messaging.ui.conversation.navigation

import androidx.navigation3.runtime.NavKey
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationNavigationReducerImplTest {

    private val reducer: ConversationNavigationReducer = ConversationNavigationReducerImpl()

    @Test
    fun navigateToConversation_replacesNewChatEntryFlowWithConversation() {
        val backStack = mutableListOf<NavKey>(NewChatNavKey)

        reducer.navigateToConversation(
            backStack = backStack,
            conversationId = CONVERSATION_ID,
        )

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

        reducer.navigateToConversation(
            backStack = backStack,
            conversationId = CONVERSATION_ID,
        )

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

        reducer.navigateToConversation(
            backStack = backStack,
            conversationId = "conversation-2",
        )

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = CONVERSATION_ID),
                ConversationNavKey(conversationId = "conversation-2"),
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

        reducer.navigateToRecipientPicker(
            backStack = backStack,
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

        reducer.navigateToAddParticipants(
            backStack = backStack,
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

        reducer.navigateToAddParticipants(
            backStack = backStack,
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
    fun popBackStack_returnsFalseWhenBackStackHasSingleEntry() {
        val backStack = mutableListOf<NavKey>(NewChatNavKey)

        val wasPopped = reducer.popBackStack(backStack = backStack)

        assertFalse(wasPopped)
        assertEquals(listOf(NewChatNavKey), backStack)
    }

    @Test
    fun popBackStack_removesLastEntryWhenBackStackHasMultipleEntries() {
        val backStack = mutableListOf(
            NewChatNavKey,
            ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        val wasPopped = reducer.popBackStack(backStack = backStack)

        assertTrue(wasPopped)
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

        reducer.replaceCurrentConversation(
            backStack = backStack,
            conversationId = "conversation-2",
        )

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = "conversation-2"),
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

        reducer.replaceCurrentConversation(
            backStack = backStack,
            conversationId = "conversation-2",
        )

        assertEquals(
            listOf(
                NewChatNavKey,
                ConversationNavKey(conversationId = "conversation-2"),
            ),
            backStack,
        )
    }

    @Test
    fun resetBackStack_keepsSingleMatchingDestinationUntouched() {
        val backStack = mutableListOf<NavKey>(NewChatNavKey)

        reducer.resetBackStack(
            backStack = backStack,
            destination = NewChatNavKey,
        )

        assertEquals(listOf(NewChatNavKey), backStack)
    }

    @Test
    fun resetBackStack_replacesExistingEntriesWithDestination() {
        val backStack = mutableListOf(
            NewChatNavKey,
            ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        reducer.resetBackStack(
            backStack = backStack,
            destination = ConversationNavKey(conversationId = "conversation-2"),
        )

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = "conversation-2"),
            ),
            backStack,
        )
    }

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
