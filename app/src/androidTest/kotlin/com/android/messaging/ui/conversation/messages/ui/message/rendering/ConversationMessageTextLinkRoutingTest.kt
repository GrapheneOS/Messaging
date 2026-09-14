package com.android.messaging.ui.conversation.messages.ui.message.rendering

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ConversationMessageTextLinkRoutingTest : BaseConversationMessageRenderingTest() {

    @Test
    fun externalLinkClick_selectionModeForwardsMessageClickOnly() {
        setConversationMessageContent(
            message = message(text = LINK_TEXT),
            isSelectionMode = true,
        )

        awaitLinkAnnotated(text = LINK_TEXT)

        composeTestRule
            .onNodeWithText(text = LINK_TEXT, useUnmergedTree = true)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageClick.invoke()
            }
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
        }
    }

    @Test
    fun externalLinkClick_resendableMessageForwardsResendOnly() {
        setConversationMessageContent(
            message = message(
                text = LINK_TEXT,
                status = ConversationMessageUiModel.Status.Outgoing.Failed,
                canResendMessage = true,
            ),
        )

        awaitLinkAnnotated(text = LINK_TEXT)

        composeTestRule
            .onNodeWithText(text = LINK_TEXT, useUnmergedTree = true)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onResendClick.invoke()
            }
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
            verify(exactly = 0) {
                onMessageClick.invoke()
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun awaitLinkAnnotated(text: String) {
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            composeTestRule
                .onAllNodesWithText(text = text, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .any { node ->
                    node.config
                        .getOrNull(SemanticsProperties.Text)
                        ?.any { it.hasLinkAnnotations(start = 0, end = it.length) } == true
                }
        }
    }

    private companion object {
        private const val LINK_TEXT = "https://example.com"
    }
}
