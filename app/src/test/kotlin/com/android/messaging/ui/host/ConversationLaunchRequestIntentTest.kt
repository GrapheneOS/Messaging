package com.android.messaging.ui.host

import android.content.Intent
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationLaunchRequestIntentTest {

    @Test
    fun hasConversationLaunchPayload_isFalseWithoutLaunchExtras() {
        val intent = Intent(Intent.ACTION_MAIN)

        assertFalse(intent.hasConversationLaunchPayload())
    }

    @Test
    fun hasConversationLaunchPayload_isTrueForAnyLaunchExtra() {
        assertTrue(intentWith(conversationId = "conversation").hasConversationLaunchPayload())
        assertTrue(intentWith(draft = MessageData()).hasConversationLaunchPayload())
        assertTrue(
            intentWith(attachmentUri = "content://attachment").hasConversationLaunchPayload()
        )
    }

    @Test
    fun toConversationLaunchRequest_mapsEveryLaunchExtra() {
        val draft = MessageData()
        val intent = intentWith(
            conversationId = "conversation",
            draft = draft,
            attachmentUri = "content://attachment",
            attachmentType = "image/jpeg",
            messagePosition = 4,
        )

        val launchRequest = intent.toConversationLaunchRequest(
            launchGeneration = 2,
            isLaunchedFromBubble = true,
        )

        assertEquals(
            ConversationEntryLaunchRequest(
                launchGeneration = 2,
                conversationId = ConversationId("conversation"),
                draftData = draft,
                startupAttachmentUri = "content://attachment",
                startupAttachmentType = "image/jpeg",
                messagePosition = 4,
                isLaunchedFromBubble = true,
            ),
            launchRequest,
        )
    }

    @Test
    fun toConversationLaunchRequest_dropsBlankAndSentinelValues() {
        val intent = intentWith(
            conversationId = "",
            attachmentUri = "",
            attachmentType = "",
            messagePosition = -1,
        )

        val launchRequest = intent.toConversationLaunchRequest(
            launchGeneration = 0,
            isLaunchedFromBubble = false,
        )

        assertNull(launchRequest.conversationId)
        assertNull(launchRequest.startupAttachmentUri)
        assertNull(launchRequest.startupAttachmentType)
        assertNull(launchRequest.messagePosition)
    }

    @Test
    fun toConversationLaunchRequest_stripsConsumedExtrasFromIntent() {
        val intent = intentWith(
            conversationId = "conversation",
            draft = MessageData(),
            attachmentUri = "content://attachment",
            messagePosition = 4,
        )

        intent.toConversationLaunchRequest(
            launchGeneration = 0,
            isLaunchedFromBubble = false,
        )

        assertTrue(intent.hasExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID))
        assertTrue(intent.hasExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_URI))
        assertFalse(intent.hasExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA))
        assertFalse(intent.hasExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION))
    }

    private fun intentWith(
        conversationId: String? = null,
        draft: MessageData? = null,
        attachmentUri: String? = null,
        attachmentType: String? = null,
        messagePosition: Int? = null,
    ): Intent {
        return Intent().apply {
            conversationId?.let { putExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID, it) }
            draft?.let { putExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA, it) }
            attachmentUri?.let { putExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_URI, it) }
            attachmentType?.let { putExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_TYPE, it) }
            messagePosition?.let { putExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION, it) }
        }
    }
}
