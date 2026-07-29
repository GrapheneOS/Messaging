package com.android.messaging.ui.conversation.navigation

import android.content.Intent
import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TEST_CONVERSATION_ID_VALUE as CONVERSATION_ID_VALUE
import com.android.messaging.ui.UIIntents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationRouteTest {

    @Test
    fun conversationRoute_withConversationId_routesToThatConversation() {
        val intent = Intent().putExtra(
            UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID,
            CONVERSATION_ID_VALUE,
        )

        assertEquals(
            listOf(ConversationNavKey(conversationId = CONVERSATION_ID)),
            conversationRoute(intent),
        )
    }

    @Test
    fun conversationRoute_withComposeNewConversationFlag_routesToNewChat() {
        val intent = Intent().putExtra(
            UIIntents.UI_INTENT_EXTRA_COMPOSE_NEW_CONVERSATION,
            true,
        )

        assertEquals(listOf<NavKey>(NewChatNavKey), conversationRoute(intent))
    }

    @Test
    fun conversationRoute_withComposeNewConversationFlagDisabled_routesNowhere() {
        val intent = Intent().putExtra(
            UIIntents.UI_INTENT_EXTRA_COMPOSE_NEW_CONVERSATION,
            false,
        )

        assertNull(conversationRoute(intent))
    }

    @Test
    fun conversationRoute_withSharedAttachmentButNoConversationId_routesToNewChat() {
        val intent = Intent().putExtra(
            UIIntents.UI_INTENT_EXTRA_ATTACHMENT_URI,
            "content://media/external/images/media/1",
        )

        assertEquals(listOf<NavKey>(NewChatNavKey), conversationRoute(intent))
    }

    @Test
    fun conversationRoute_withDraftDataButNoConversationId_routesToNewChat() {
        val intent = Intent().putExtra(
            UIIntents.UI_INTENT_EXTRA_DRAFT_DATA,
            MessageData(),
        )

        assertEquals(listOf<NavKey>(NewChatNavKey), conversationRoute(intent))
    }

    @Test
    fun conversationRoute_withConversationIdAndAttachment_prefersTheConversation() {
        val intent = Intent()
            .putExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID, CONVERSATION_ID_VALUE)
            .putExtra(
                UIIntents.UI_INTENT_EXTRA_ATTACHMENT_URI,
                "content://media/external/images/media/1",
            )

        assertEquals(
            listOf(ConversationNavKey(conversationId = CONVERSATION_ID)),
            conversationRoute(intent),
        )
    }

    @Test
    fun conversationRoute_withBlankConversationId_routesToNewChat() {
        val intent = Intent().putExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID, "")

        assertEquals(listOf(NewChatNavKey), conversationRoute(intent))
    }

    @Test
    fun conversationRoute_withoutAnyLaunchExtras_routesNowhere() {
        assertNull(conversationRoute(Intent()))
    }

    @Test
    fun conversationRoute_withUnrelatedExtras_routesNowhere() {
        val intent = Intent().putExtra(
            UIIntents.UI_INTENT_EXTRA_GOTO_CONVERSATION_LIST,
            true,
        )

        assertNull(conversationRoute(intent))
    }
}
