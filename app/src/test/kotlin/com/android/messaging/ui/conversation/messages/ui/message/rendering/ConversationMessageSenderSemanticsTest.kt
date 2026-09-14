package com.android.messaging.ui.conversation.messages.ui.message.rendering

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.conversationMessageBubbleTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageSenderSemanticsTest : BaseConversationMessageRenderingTest() {

    @Test
    fun incomingMessageWithoutAVisibleSenderIsStillAnnouncedWithOne() {
        setConversationMessageContent(
            message = incomingMessage(),
            showIncomingParticipantIdentity = false,
        )

        assertEquals(
            listOf(
                AnnotatedString(text = incomingAnnouncement(sender = SENDER_DISPLAY_NAME)),
                AnnotatedString(text = DEFAULT_BODY_TEXT),
            ),
            bubbleText(),
        )
    }

    @Test
    fun incomingMessageWithoutASenderNameFallsBackToTheUnknownSender() {
        setConversationMessageContent(
            message = incomingMessage(senderDisplayName = null),
            showIncomingParticipantIdentity = false,
        )

        assertEquals(
            listOf(
                AnnotatedString(
                    text = incomingAnnouncement(
                        sender = targetContext.getString(R.string.unknown_sender),
                    ),
                ),
                AnnotatedString(text = DEFAULT_BODY_TEXT),
            ),
            bubbleText(),
        )
    }

    @Test
    fun clusteredIncomingMessageIsAnnouncedWithTheSenderItNoLongerShows() {
        setConversationMessageContent(
            message = incomingMessage(canClusterWithPrevious = true),
            showIncomingParticipantIdentity = true,
        )

        assertEquals(
            listOf(
                AnnotatedString(text = incomingAnnouncement(sender = SENDER_DISPLAY_NAME)),
                AnnotatedString(text = DEFAULT_BODY_TEXT),
            ),
            bubbleText(),
        )
    }

    @Test
    fun visibleSenderNameIsNotAnnouncedTwice() {
        setConversationMessageContent(
            message = incomingMessage(),
            showIncomingParticipantIdentity = true,
        )

        assertEquals(
            listOf(
                AnnotatedString(text = SENDER_DISPLAY_NAME),
                AnnotatedString(text = DEFAULT_BODY_TEXT),
            ),
            bubbleText(),
        )
    }

    @Test
    fun outgoingMessageIsAnnouncedAsSentByTheUser() {
        setConversationMessageContent(message = message(isIncoming = false))

        assertEquals(
            listOf(
                AnnotatedString(
                    text = targetContext.getString(R.string.outgoing_sender_content_description),
                ),
                AnnotatedString(text = DEFAULT_BODY_TEXT),
            ),
            bubbleText(),
        )
    }

    private fun incomingMessage(
        senderDisplayName: String? = SENDER_DISPLAY_NAME,
        canClusterWithPrevious: Boolean = false,
    ): ConversationMessageUiModel {
        return message(
            status = ConversationMessageUiModel.Status.Incoming.Complete,
            isIncoming = true,
            senderDisplayName = senderDisplayName,
            canClusterWithPrevious = canClusterWithPrevious,
        )
    }

    private fun incomingAnnouncement(sender: String): String {
        return targetContext.getString(R.string.incoming_sender_content_description, sender)
    }

    private fun bubbleText(): List<AnnotatedString> {
        return composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageBubbleTestTag(
                    messageId = MessageId(DEFAULT_MESSAGE_ID),
                ),
            )
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
    }

    private companion object {
        private const val SENDER_DISPLAY_NAME = "Ada Lovelace"
    }
}
