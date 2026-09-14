package com.android.messaging.ui.conversation.entry

import android.content.Intent
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TEST_CONVERSATION_ID_VALUE as CONVERSATION_ID_VALUE
import com.android.messaging.testutil.assertThat
import com.android.messaging.ui.UIIntents
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationLaunchRequestIntentTest {

    @Test
    fun hasConversationLaunchPayload_withConversationId_reportsPayload() {
        val intent = Intent().putExtra(
            UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID,
            CONVERSATION_ID_VALUE,
        )

        assertTrue(intent.hasConversationLaunchPayload())
    }

    @Test
    fun hasConversationLaunchPayload_withDraftData_reportsPayload() {
        val intent = Intent().putExtra(
            UIIntents.UI_INTENT_EXTRA_DRAFT_DATA,
            MessageData(),
        )

        assertTrue(intent.hasConversationLaunchPayload())
    }

    @Test
    fun hasConversationLaunchPayload_withAttachmentUri_reportsPayload() {
        val intent = Intent().putExtra(
            UIIntents.UI_INTENT_EXTRA_ATTACHMENT_URI,
            ATTACHMENT_URI,
        )

        assertTrue(intent.hasConversationLaunchPayload())
    }

    @Test
    fun hasConversationLaunchPayload_withoutLaunchExtras_reportsNoPayload() {
        assertFalse(Intent().hasConversationLaunchPayload())
    }

    @Test
    fun hasConversationLaunchPayload_withAttachmentTypeOnly_reportsNoPayload() {
        val intent = Intent().putExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_TYPE, "image/png")

        assertFalse(intent.hasConversationLaunchPayload())
    }

    @Test
    fun toConversationLaunchRequest_mapsEveryPopulatedExtra() {
        val intent = Intent()
            .putExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID, CONVERSATION_ID_VALUE)
            .putExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA, MessageData())
            .putExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_URI, ATTACHMENT_URI)
            .putExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_TYPE, "image/png")
            .putExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION, 4)

        val request = intent.toConversationLaunchRequest()

        assertThat(request.conversationId).isEqualTo(CONVERSATION_ID)
        assertNotNull(request.draftData)
        assertThat(request.startupAttachmentUri).isEqualTo(ATTACHMENT_URI)
        assertThat(request.startupAttachmentType).isEqualTo("image/png")
        assertThat(request.messagePosition).isEqualTo(4)
    }

    @Test
    fun toConversationLaunchRequest_withoutExtras_mapsEveryFieldToNull() {
        val request = Intent().toConversationLaunchRequest()

        assertNull(request.conversationId)
        assertNull(request.draftData)
        assertNull(request.startupAttachmentUri)
        assertNull(request.startupAttachmentType)
        assertNull(request.messagePosition)
    }

    @Test
    fun toConversationLaunchRequest_withEmptyAttachmentExtras_mapsThemToNull() {
        val intent = Intent()
            .putExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_URI, "")
            .putExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_TYPE, "")

        val request = intent.toConversationLaunchRequest()

        assertNull(request.startupAttachmentUri)
        assertNull(request.startupAttachmentType)
    }

    @Test
    fun toConversationLaunchRequest_withNegativeMessagePosition_mapsItToNull() {
        val intent = Intent().putExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION, -1)

        assertNull(intent.toConversationLaunchRequest().messagePosition)
    }

    @Test
    fun toConversationLaunchRequest_withFirstMessagePosition_keepsTheBoundaryValue() {
        val intent = Intent().putExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION, 0)

        assertThat(intent.toConversationLaunchRequest().messagePosition).isEqualTo(0)
    }

    @Test
    fun toConversationLaunchRequest_stripsTheOneShotExtrasFromTheIntent() {
        val intent = Intent()
            .putExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID, CONVERSATION_ID_VALUE)
            .putExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA, MessageData())
            .putExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION, 4)

        intent.toConversationLaunchRequest()

        assertFalse(intent.hasExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA))
        assertFalse(intent.hasExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION))
        assertTrue(intent.hasExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID))
    }

    private companion object {
        const val ATTACHMENT_URI = "content://media/external/images/media/1"
    }
}
