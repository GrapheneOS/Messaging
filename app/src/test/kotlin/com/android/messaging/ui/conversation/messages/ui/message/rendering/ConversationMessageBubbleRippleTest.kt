package com.android.messaging.ui.conversation.messages.ui.message.rendering

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.messages.ui.message.ConversationMessageBubbleRipple
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageBubbleRippleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val bubbleRipple = ConversationMessageBubbleRipple()
    private val bubbleInteractions = mutableListOf<Interaction>()

    @Test
    fun pressOnTheBubbleStartsTheRippleUnderTheFinger() {
        setMessageWithBubbleOnTheRightHalf()

        clickMessageAt(widthFraction = 0.75f)

        composeTestRule.runOnIdle {
            val press = bubbleInteractions.first() as PressInteraction.Press
            val expectedPosition = with(composeTestRule.density) {
                Offset(x = 50.dp.toPx(), y = 50.dp.toPx())
            }

            assertEquals(expectedPosition, press.pressPosition)
            assertSame(press, (bubbleInteractions.last() as PressInteraction.Release).press)
            assertEquals(2, bubbleInteractions.size)
        }
    }

    @Test
    fun pressBesideTheBubbleShowsNoRipple() {
        setMessageWithBubbleOnTheRightHalf()

        clickMessageAt(widthFraction = 0.25f)

        composeTestRule.runOnIdle {
            assertTrue(bubbleInteractions.isEmpty())
        }
    }

    private fun setMessageWithBubbleOnTheRightHalf() {
        composeTestRule.setContent {
            LaunchedEffect(bubbleRipple) {
                launch {
                    bubbleRipple.bubbleInteractionSource.interactions.collect { interaction ->
                        bubbleInteractions += interaction
                    }
                }
                bubbleRipple.replayPressesOnBubble()
            }

            Row(
                modifier = Modifier
                    .testTag(tag = MESSAGE_TAG)
                    .size(width = 200.dp, height = 100.dp)
                    .onPlaced { coordinates -> bubbleRipple.messageCoordinates = coordinates }
                    .clickable(
                        interactionSource = bubbleRipple.messageInteractionSource,
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Spacer(modifier = Modifier.size(size = 100.dp))
                Spacer(
                    modifier = Modifier
                        .size(size = 100.dp)
                        .onPlaced { coordinates -> bubbleRipple.bubbleCoordinates = coordinates },
                )
            }
        }
    }

    private fun clickMessageAt(widthFraction: Float) {
        composeTestRule
            .onNodeWithTag(testTag = MESSAGE_TAG)
            .performTouchInput {
                click(position = Offset(x = width * widthFraction, y = height / 2f))
            }
    }

    private companion object {
        private const val MESSAGE_TAG = "message"
    }
}
