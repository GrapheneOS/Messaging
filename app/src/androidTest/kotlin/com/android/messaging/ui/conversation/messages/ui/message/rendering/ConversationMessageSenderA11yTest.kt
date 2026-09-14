package com.android.messaging.ui.conversation.messages.ui.message.rendering

import android.view.accessibility.AccessibilityNodeInfo
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
                dumpTree(),
            bubbleNodes.isNotEmpty(),
        )

        assertTrue(
            "TalkBack cannot tell who sent this message: the focusable node speaking \"$body\" " +
                "never says \"$announcement\". Nodes: " +
                bubbleNodes.joinToString { it.dumpSubtree() },
            bubbleNodes.any { node -> node.subtreeText().contains(announcement) },
        )
    }

    /** The bubbles TalkBack would stop on and read the body out from. */
    private fun awaitFocusableNodesSpeaking(text: String): List<AccessibilityNodeInfo> {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        repeat(A11Y_TREE_POLL_ATTEMPTS) {
            val matches = uiAutomation.rootInActiveWindow?.collectNodes().orEmpty().filter { node ->
                node.isScreenReaderFocusable && node.subtreeText().contains(text)
            }
            if (matches.isNotEmpty()) {
                return matches
            }
            Thread.sleep(A11Y_TREE_POLL_INTERVAL_MILLIS)
        }
        return emptyList()
    }

    private fun dumpTree(): String {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        return uiAutomation.rootInActiveWindow?.dumpSubtree() ?: "<no active window>"
    }

    private fun AccessibilityNodeInfo.subtreeText(): String {
        return collectNodes().joinToString(separator = " ") { node ->
            listOfNotNull(node.text, node.contentDescription).joinToString(separator = " ")
        }
    }

    private fun AccessibilityNodeInfo.dumpSubtree(): String {
        return collectNodes().joinToString(separator = " / ") { it.describe() }
    }

    private fun AccessibilityNodeInfo.collectNodes(): List<AccessibilityNodeInfo> {
        return buildList {
            add(this@collectNodes)
            repeat(childCount) { index ->
                getChild(index)?.let { addAll(it.collectNodes()) }
            }
        }
    }

    private fun AccessibilityNodeInfo.describe(): String {
        return "[text=$text, contentDescription=$contentDescription, " +
            "screenReaderFocusable=$isScreenReaderFocusable]"
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
        private const val A11Y_TREE_POLL_ATTEMPTS = 20
        private const val A11Y_TREE_POLL_INTERVAL_MILLIS = 250L
    }
}
