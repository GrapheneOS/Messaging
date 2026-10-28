package com.android.messaging.ui.conversation.navigation

import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversationlist.navigation.ArchivedConversationListNavKey
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class ConversationPaneMetadataTest {

    @Test
    fun detailPaneConversationId_conversationOnTop_returnsItsId() {
        val backStack = listOf(
            ConversationListNavKey,
            ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        assertEquals(CONVERSATION_ID, detailPaneConversationId(backStack = backStack))
    }

    @Test
    fun detailPaneConversationId_newChatOverConversation_returnsNull() {
        val backStack = listOf(
            ConversationListNavKey,
            ConversationNavKey(conversationId = CONVERSATION_ID),
            NewChatNavKey,
        )

        assertNull(detailPaneConversationId(backStack = backStack))
    }

    @Test
    fun detailPaneConversationId_listPaneOverConversation_returnsTheConversation() {
        val backStack = listOf(
            ConversationListNavKey,
            ConversationNavKey(conversationId = CONVERSATION_ID),
            ArchivedConversationListNavKey,
        )

        assertEquals(CONVERSATION_ID, detailPaneConversationId(backStack = backStack))
    }

    @Test
    fun detailPaneConversationId_withoutDetailDestination_returnsNull() {
        assertNull(detailPaneConversationId(backStack = listOf(ConversationListNavKey)))
    }
}
