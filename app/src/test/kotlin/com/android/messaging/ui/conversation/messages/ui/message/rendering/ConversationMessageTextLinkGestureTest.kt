package com.android.messaging.ui.conversation.messages.ui.message.rendering

import android.content.Context
import android.os.SystemClock
import android.view.ViewConfiguration
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextLinks
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageTextLinkGestureTest : BaseConversationMessageRenderingTest() {

    @Before
    fun classifyAllTextAsUrl() {
        ApplicationProvider
            .getApplicationContext<Context>()
            .getSystemService(TextClassificationManager::class.java)
            .setTextClassifier(
                object : TextClassifier {
                    override fun generateLinks(request: TextLinks.Request): TextLinks {
                        return TextLinks.Builder(request.text.toString())
                            .addLink(
                                0,
                                request.text.length,
                                mapOf(TextClassifier.TYPE_URL to 1f),
                            )
                            .build()
                    }
                },
            )
    }

    @Test
    fun touchExploration_linkTapOfMessageWithoutTapNeitherOpensNorSelects() {
        enableTouchExploration()
        setLinkMessageContent()

        clickLinkText()
        composeTestRule.mainClock.advanceTimeBy(
            milliseconds = ViewConfiguration.getLongPressTimeout() * 2L,
        )

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
            verify(exactly = 0) {
                onMessageLongClick.invoke()
            }
        }
    }

    @Test
    fun linkLongClick_laterLinkTapOpensTheLink() {
        setLinkMessageContent()

        composeTestRule
            .onNodeWithText(text = LINK_TEXT, useUnmergedTree = true)
            .performTouchInput { longClick(position = centerLeft) }
        // Past the time a tap on the link is taken for the end of the long press
        SystemClock.sleep(LINK_CLICK_SUPPRESSION_ELAPSED_MILLIS)
        clickLinkText()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onMessageLongClick.invoke()
            }
            verify(exactly = 1) {
                onExternalUriClick.invoke(any())
            }
        }
    }

    private fun setLinkMessageContent() {
        setConversationMessageContent(
            message = message(text = LINK_TEXT),
        )
        awaitLinkAnnotated(text = LINK_TEXT)
    }

    private fun clickLinkText() {
        composeTestRule
            .onNodeWithText(text = LINK_TEXT, useUnmergedTree = true)
            .performTouchInput { click(position = centerLeft) }
    }

    private companion object {
        private const val LINK_CLICK_SUPPRESSION_ELAPSED_MILLIS = 1_000L
        private const val LINK_TEXT = "https://example.com"
    }
}
