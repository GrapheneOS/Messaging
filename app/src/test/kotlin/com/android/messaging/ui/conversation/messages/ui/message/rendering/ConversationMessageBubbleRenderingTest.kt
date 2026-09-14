package com.android.messaging.ui.conversation.messages.ui.message.rendering

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageBubbleRenderingTest : BaseConversationMessageRenderingTest() {

    @Test
    fun incomingTextMessage_showsSenderBodyAndSimMetadata() {
        val simAnnotation = targetContext.getString(
            R.string.conversation_message_sim_annotation,
            SIM_DISPLAY_NAME,
        )

        setConversationMessageContent(
            message = message(
                text = INCOMING_BODY_TEXT,
                status = ConversationMessageUiModel.Status.Incoming.Complete,
                isIncoming = true,
                senderDisplayName = SENDER_DISPLAY_NAME,
            ),
            simDisplayName = SIM_DISPLAY_NAME,
        )

        composeTestRule
            .onNodeWithText(text = SENDER_DISPLAY_NAME)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = INCOMING_BODY_TEXT)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = simAnnotation, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun outgoingTextMessage_hidesIncomingIdentity() {
        setConversationMessageContent(
            message = message(
                text = OUTGOING_BODY_TEXT,
                isIncoming = false,
                senderDisplayName = SENDER_DISPLAY_NAME,
            ),
        )

        composeTestRule
            .onNodeWithText(text = OUTGOING_BODY_TEXT)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = SENDER_DISPLAY_NAME)
            .assertDoesNotExist()
    }

    @Test
    fun mmsDownloadMessage_rendersDownloadBodyInsteadOfRegularBody() {
        setConversationMessageContent(
            message = message(
                text = HIDDEN_MMS_TEXT,
                status = ConversationMessageUiModel.Status.Incoming.YetToManualDownload,
                isIncoming = true,
                canDownloadMessage = true,
                mmsDownload = mmsDownload(
                    state = MmsDownloadUiModel.State.AwaitingManualDownload,
                ),
                protocol = ConversationMessageUiModel.Protocol.MMS_PUSH_NOTIFICATION,
            ),
        )

        composeTestRule
            .onNodeWithText(text = targetContext.getString(R.string.message_title_manual_download))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = targetContext.getString(R.string.message_status_download))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = HIDDEN_MMS_TEXT)
            .assertDoesNotExist()
    }

    @Test
    fun attachmentWithSenderSubjectAndBody_rendersHeaderMediaAndFooter() {
        setConversationMessageContent(
            message = message(
                text = ATTACHMENT_BODY_TEXT,
                parts = persistentListOf(
                    imagePart(),
                ),
                status = ConversationMessageUiModel.Status.Incoming.Complete,
                isIncoming = true,
                senderDisplayName = SENDER_DISPLAY_NAME,
                mmsSubject = MMS_SUBJECT,
                protocol = ConversationMessageUiModel.Protocol.MMS,
            ),
        )

        composeTestRule
            .onNodeWithText(text = SENDER_DISPLAY_NAME)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = MMS_SUBJECT)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = ATTACHMENT_BODY_TEXT)
            .assertIsDisplayed()
    }

    @Test
    fun clusteredIncomingMessage_hidesSenderAndAvatarInitial() {
        setConversationMessageContent(
            message = message(
                text = INCOMING_BODY_TEXT,
                status = ConversationMessageUiModel.Status.Incoming.Complete,
                isIncoming = true,
                senderDisplayName = "Zoe",
                canClusterWithPrevious = true,
                canClusterWithNext = true,
            ),
        )

        composeTestRule
            .onNodeWithText(text = INCOMING_BODY_TEXT)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Zoe")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(text = "Z")
            .assertDoesNotExist()
    }

    private companion object {
        private const val ATTACHMENT_BODY_TEXT = "Photo caption body"
        private const val HIDDEN_MMS_TEXT = "This text should not render for MMS notification"
        private const val INCOMING_BODY_TEXT = "Incoming message body"
        private const val MMS_SUBJECT = "Trip photos"
        private const val OUTGOING_BODY_TEXT = "Outgoing message body"
        private const val SENDER_DISPLAY_NAME = "Alice Rivera"
    }
}
