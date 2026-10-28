package com.android.messaging.ui.conversation.navigation

import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.assertThat
import com.android.messaging.ui.conversation.entry.ConversationEntryScreenModel
import com.android.messaging.ui.navigation.NavigationReducerImpl
import com.android.messaging.ui.navigation.NavigatorImpl
import com.android.messaging.ui.vcarddetail.navigation.VCardDetailNavKey
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationNavigatorImplTest {

    private val entryModel = mockk<ConversationEntryScreenModel>(relaxed = true)

    @Test
    fun navigateToConversation_withTargetMessage_asksTheConversationToOpenAtIt() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = ConversationId("conversation-2")),
        )

        navigator(backStack = backStack).navigateToConversation(
            conversationId = CONVERSATION_ID,
            targetMessageId = TARGET_MESSAGE_ID,
        )

        verify(exactly = 1) {
            entryModel.onConversationNavigationRequested(
                conversationId = CONVERSATION_ID,
                pendingSelfParticipantId = null,
                targetMessageId = TARGET_MESSAGE_ID,
            )
        }
        assertEquals(
            listOf(ConversationNavKey(conversationId = CONVERSATION_ID)),
            backStack,
        )
    }

    @Test
    fun navigateToConversation_withoutTargetMessage_leavesTheEntryStateAlone() {
        navigator(backStack = mutableListOf()).navigateToConversation(
            conversationId = CONVERSATION_ID,
        )

        verify(exactly = 0) {
            entryModel.onConversationNavigationRequested(
                conversationId = any(),
                pendingSelfParticipantId = any(),
                targetMessageId = any(),
            )
        }
    }

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
    fun navigateToConversation_replacesConversationAlreadyOnTop() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        navigator(backStack = backStack).navigateToConversation(
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
        val backStack = mutableListOf<NavKey>(
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
    fun replaceWithConversation_swapsTheCurrentDestination() {
        val backStack = mutableListOf<NavKey>(NewChatNavKey)

        navigator(backStack = backStack).replaceWithConversation(
            conversationId = CONVERSATION_ID,
        )

        assertEquals(
            listOf(ConversationNavKey(conversationId = CONVERSATION_ID)),
            backStack,
        )
    }

    @Test
    fun replaceCurrentConversation_removesAddParticipantsAndReplacesExistingConversation() {
        val backStack = mutableListOf<NavKey>(
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
        val backStack = mutableListOf<NavKey>(
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

    @Test
    fun navigateToVCardDetail_appendsVCardDetailDestination() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = ConversationId("c")),
        )

        navigator(backStack = backStack).navigateToVCardDetail(
            uri = "content://scratch/contact.vcf",
        )

        assertThat(backStack.last()).isEqualTo(
            VCardDetailNavKey(uri = "content://scratch/contact.vcf"),
        )
        assertEquals(2, backStack.size)
    }

    private fun navigator(backStack: MutableList<NavKey>): ConversationNavigator {
        return ConversationNavigatorImpl(
            navigator = NavigatorImpl(
                backStack = backStack,
                navigationReducer = NavigationReducerImpl(),
                onFinish = {},
            ),
            entryModel = entryModel,
        )
    }

    private companion object {
        private val TARGET_MESSAGE_ID = MessageId("message-7")
    }
}
