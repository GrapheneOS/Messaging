package com.android.messaging.ui.conversation.messages.ui.message.rendering

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.R
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.screen.model.ConversationMessageAction
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ConversationMessageActionsA11yTest : BaseConversationMessageRenderingTest() {

    @Test
    fun screenReaderCanCopyTheMessageFromTheBubbleItFocuses() {
        setConversationMessageContent(message = message(text = MESSAGE_TEXT))
        composeTestRule.waitForIdle()

        val bubbleNodes = awaitFocusableNodesSpeaking(text = MESSAGE_TEXT)
        assertTrue(
            "No screen reader focusable node speaks \"$MESSAGE_TEXT\". Tree: " +
                dumpActiveWindow(),
            bubbleNodes.isNotEmpty(),
        )

        val copyLabel = label(action = ConversationMessageAction.Copy)
        val detailsLabel = label(action = ConversationMessageAction.Details)
        val bubble = bubbleNodes.firstOrNull { node ->
            val labels = node.actionList.map { it.label?.toString() }
            copyLabel in labels && detailsLabel in labels
        }
        assertNotNull(
            "The focusable node speaking \"$MESSAGE_TEXT\" offers no \"$copyLabel\" and " +
                "\"$detailsLabel\" actions. Nodes: " +
                bubbleNodes.joinToString { it.dumpSubtree() },
            bubble,
        )

        val copyAction = bubble!!.actionList.single { it.label?.toString() == copyLabel }
        assertTrue(bubble.performAction(copyAction.id))

        verify(timeout = ACTION_TIMEOUT_MILLIS) {
            onMessageActionClick(ConversationMessageAction.Copy)
        }
    }

    @Test
    fun statusIsSpokenWithTheMessageInsteadOfAsItsOwnStop() {
        setConversationMessageContent(
            message = message(
                text = MESSAGE_TEXT,
                status = ConversationMessageUiModel.Status.Outgoing.Delivered,
            ),
        )
        composeTestRule.waitForIdle()

        val deliveredStatus = InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(R.string.delivered_status_content_description)
        val statusNodes = awaitFocusableNodesSpeaking(text = deliveredStatus)

        assertTrue(
            "TalkBack stops on \"$deliveredStatus\" apart from the message it belongs to, " +
                "where no message actions are offered. Nodes: " +
                statusNodes.joinToString { it.dumpSubtree() },
            statusNodes.isNotEmpty() &&
                statusNodes.all { node -> node.subtreeText().contains(MESSAGE_TEXT) },
        )
    }

    @Test
    fun messageIsNotReadBeforeTheAttachmentInsideIt() {
        setConversationMessageContent(
            message = message(text = MESSAGE_TEXT, parts = persistentListOf(imagePart())),
        )
        composeTestRule.waitForIdle()

        val bubble = awaitFocusableNodesSpeaking(text = MESSAGE_TEXT).single()
        val next = bubble.traversalBefore

        assertFalse(
            "The message is read right before a node inside it, which TalkBack ignores, " +
                "reading the message out of order in the list. Next: " + next?.dumpSubtree(),
            next != null && bubble.hasDescendant(node = next),
        )
    }

    private fun label(action: ConversationMessageAction): String {
        return InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(action.labelRes)
    }

    private companion object {
        private const val MESSAGE_TEXT = "Meet me at the station."
        private const val ACTION_TIMEOUT_MILLIS = 5_000L
    }
}
