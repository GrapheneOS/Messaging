package com.android.messaging.ui.conversation.messages.ui.message.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.conversationMessageBubbleTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.ui.message.ConversationMessage
import com.android.messaging.ui.conversation.preview.previewImagePart
import com.android.messaging.ui.conversation.preview.previewIncomingMessage
import com.android.messaging.ui.conversation.preview.previewOutgoingMessage
import com.android.messaging.ui.core.AppTheme
import kotlin.math.roundToInt
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageHighlightRenderingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun conversationMessage_highlightedText_tintsOnlyItsBubble() {
        setMessagesContent(message = ::textMessage, isHighlighted = { true })

        assertNotEquals(
            bubbleEndPixel(messageId = PLAIN_MESSAGE_ID),
            bubbleEndPixel(messageId = HIGHLIGHTED_MESSAGE_ID),
        )
        // Beside the bubble, and on the timestamp line below it.
        assertEquals(AndroidColor.WHITE, rowStartPixel(messageTag = HIGHLIGHTED_MESSAGE_TAG))
        assertEquals(
            AndroidColor.WHITE,
            rowStartPixel(messageTag = HIGHLIGHTED_MESSAGE_TAG, atBottom = true),
        )
    }

    @Test
    fun conversationMessage_highlightedMediaOnly_tintsTheMedia() {
        setMessagesContent(message = ::imageOnlyMessage, isHighlighted = { true })

        assertNotEquals(
            bubbleCenterPixel(messageId = PLAIN_MESSAGE_ID),
            bubbleCenterPixel(messageId = HIGHLIGHTED_MESSAGE_ID),
        )
    }

    @Test
    fun conversationMessage_highlightEnded_fadesTheBubbleBackToItsOwnColor() {
        var isHighlighted by mutableStateOf(true)
        setMessagesContent(message = ::textMessage, isHighlighted = { isHighlighted })
        val plainBubblePixel = bubbleEndPixel(messageId = PLAIN_MESSAGE_ID)
        assertNotEquals(plainBubblePixel, bubbleEndPixel(messageId = HIGHLIGHTED_MESSAGE_ID))

        composeTestRule.runOnIdle { isHighlighted = false }
        composeTestRule.waitForIdle()

        assertEquals(plainBubblePixel, bubbleEndPixel(messageId = HIGHLIGHTED_MESSAGE_ID))
    }

    private fun setMessagesContent(
        message: (MessageId) -> ConversationMessageUiModel,
        isHighlighted: () -> Boolean,
    ) {
        composeTestRule.setContent {
            AppTheme {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color.White),
                ) {
                    ConversationMessage(
                        modifier = Modifier.testTag(tag = PLAIN_MESSAGE_TAG),
                        message = message(PLAIN_MESSAGE_ID),
                    )
                    ConversationMessage(
                        modifier = Modifier.testTag(tag = HIGHLIGHTED_MESSAGE_TAG),
                        message = message(HIGHLIGHTED_MESSAGE_ID),
                        isHighlighted = isHighlighted(),
                    )
                }
            }
        }
    }

    private fun textMessage(messageId: MessageId): ConversationMessageUiModel {
        return previewOutgoingMessage(messageId = messageId)
    }

    private fun imageOnlyMessage(messageId: MessageId): ConversationMessageUiModel {
        return previewIncomingMessage(
            messageId = messageId,
            text = null,
            parts = persistentListOf(previewImagePart(text = null)),
            protocol = ConversationMessageUiModel.Protocol.MMS,
        )
    }

    /** Inside the bubble past the end of its text, where only the bubble itself is drawn. */
    private fun bubbleEndPixel(messageId: MessageId): Int {
        val bounds = bubbleBounds(messageId = messageId)

        return pixelAt(x = bounds.right - BUBBLE_END_INSET_PX, y = bounds.center.y)
    }

    private fun bubbleCenterPixel(messageId: MessageId): Int {
        val bounds = bubbleBounds(messageId = messageId)

        return pixelAt(x = bounds.center.x, y = bounds.center.y)
    }

    /** An outgoing bubble sits at the end of its row, so the start of the row is bare. */
    private fun rowStartPixel(messageTag: String, atBottom: Boolean = false): Int {
        val bounds = composeTestRule
            .onNodeWithTag(messageTag)
            .fetchSemanticsNode()
            .boundsInRoot

        return pixelAt(
            x = bounds.left + ROW_START_INSET_PX,
            y = when {
                atBottom -> bounds.bottom - ROW_START_INSET_PX
                else -> bounds.center.y
            },
        )
    }

    private fun bubbleBounds(messageId: MessageId): Rect {
        return composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageBubbleTestTag(messageId = messageId),
                useUnmergedTree = true,
            )
            .fetchSemanticsNode()
            .boundsInRoot
    }

    private fun pixelAt(x: Float, y: Float): Int {
        val bitmap = composeTestRule.runOnIdle { captureActivityBitmap() }

        return bitmap.getPixel(x.roundToInt(), y.roundToInt())
    }

    private fun captureActivityBitmap(): Bitmap {
        val view = composeTestRule.activity.window.decorView.rootView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    private companion object {
        private val PLAIN_MESSAGE_ID = MessageId("plain")
        private val HIGHLIGHTED_MESSAGE_ID = MessageId("highlighted")
        private const val PLAIN_MESSAGE_TAG = "plain_message"
        private const val HIGHLIGHTED_MESSAGE_TAG = "highlighted_message"
        private const val BUBBLE_END_INSET_PX = 6f
        private const val ROW_START_INSET_PX = 4f
    }
}
