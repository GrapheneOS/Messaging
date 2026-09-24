package com.android.messaging.ui.conversation.messages.ui.message.rendering

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.R
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ConversationMessageSenderA11yTest : BaseConversationMessageRenderingTest() {

    @Test
    fun incomingMessageIsAnnouncedWithItsSender() {
        setConversationMessageContent(
            message = message(
                text = INCOMING_TEXT,
                status = ConversationMessageUiModel.Status.Incoming.Complete,
                isIncoming = true,
                senderDisplayName = SENDER_DISPLAY_NAME,
            ),
            showIncomingParticipantIdentity = false,
        )

        assertAnnouncedWithTheBody(
            body = INCOMING_TEXT,
            announcement = string(
                resourceId = R.string.incoming_sender_content_description,
                SENDER_DISPLAY_NAME,
            ),
        )
    }

    @Test
    fun outgoingMessageIsAnnouncedAsSentByTheUser() {
        setConversationMessageContent(message = message(text = OUTGOING_TEXT))

        assertAnnouncedWithTheBody(
            body = OUTGOING_TEXT,
            announcement = string(resourceId = R.string.outgoing_sender_content_description),
        )
    }

    private fun assertAnnouncedWithTheBody(body: String, announcement: String) {
        composeTestRule.waitForIdle()

        val bubbleNodes = awaitFocusableNodesSpeaking(text = body)

        assertTrue(
            "No screen reader focusable node speaks the message body \"$body\" at all. Tree: " +
                dumpActiveWindow(),
            bubbleNodes.isNotEmpty(),
        )

        assertTrue(
            "TalkBack cannot tell who sent this message: the focusable node speaking \"$body\" " +
                "never says \"$announcement\". Nodes: " +
                bubbleNodes.joinToString { it.dumpSubtree() },
            bubbleNodes.any { node -> node.subtreeText().contains(announcement) },
        )
    }

    private fun string(resourceId: Int, vararg formatArgs: Any): String {
        return InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(resourceId, *formatArgs)
    }

    private companion object {
        private const val INCOMING_TEXT = "Can you review this before tonight?"
        private const val OUTGOING_TEXT = "I am on my way."
        private const val SENDER_DISPLAY_NAME = "Ada Lovelace"
    }
}
