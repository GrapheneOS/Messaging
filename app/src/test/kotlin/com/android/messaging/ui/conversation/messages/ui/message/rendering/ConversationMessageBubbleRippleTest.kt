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
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.messages.ui.message.ConversationMessageBubbleRipple
import com.android.messaging.ui.conversation.messages.ui.message.conversationMessageBubbleClickable
import com.android.messaging.ui.conversation.messages.ui.message.conversationMessageNonTouchClickable
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
    private var longPressCount = 0

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

    @Test
    fun tapOnAMessageWithoutATapStillRipplesTheBubble() {
        setMessageWithBubbleOnTheRightHalf(hasTap = false)

        clickMessageAt(widthFraction = 0.75f)

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(PressInteraction.Press::class, PressInteraction.Release::class),
                bubbleInteractions.map { it::class },
            )
        }
    }

    @Test
    fun longPressOnAMessageWithoutATapSelectsItAndRipplesUntilRelease() {
        setMessageWithBubbleOnTheRightHalf(hasTap = false)

        composeTestRule
            .onNodeWithTag(testTag = MESSAGE_TAG)
            .performTouchInput {
                longClick(position = Offset(x = width * 0.75f, y = height / 2f))
            }

        composeTestRule.runOnIdle {
            assertEquals(1, longPressCount)
            assertEquals(
                listOf(PressInteraction.Press::class, PressInteraction.Release::class),
                bubbleInteractions.map { it::class },
            )
        }
    }

    @Test
    fun pressLeavingAMessageWithoutATapBeforeTheTapTimeoutShowsNoRipple() {
        setMessageWithBubbleOnTheRightHalf(hasTap = false)

        composeTestRule
            .onNodeWithTag(testTag = MESSAGE_TAG)
            .performTouchInput {
                down(position = Offset(x = width * 0.75f, y = height / 2f))
                moveTo(position = Offset(x = width * 0.75f, y = height * 3f))
                up()
            }

        composeTestRule.runOnIdle {
            assertTrue(bubbleInteractions.isEmpty())
            assertEquals(0, longPressCount)
        }
    }

    @Test
    fun pressLeavingTheBubbleSidewaysCancelsTheLongPress() {
        setMessageWithBubbleOnTheRightHalf(hasTap = false)

        composeTestRule
            .onNodeWithTag(testTag = MESSAGE_TAG)
            .performTouchInput {
                down(position = Offset(x = width * 0.75f, y = height / 2f))
                advanceEventTime(durationMillis = 200)
                moveTo(position = Offset(x = width * 0.25f, y = height / 2f))
                advanceEventTime(durationMillis = viewConfiguration.longPressTimeoutMillis * 2)
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(0, longPressCount)
            assertEquals(
                listOf(PressInteraction.Press::class, PressInteraction.Cancel::class),
                bubbleInteractions.map { it::class },
            )
        }
    }

    private fun setMessageWithBubbleOnTheRightHalf(hasTap: Boolean = true) {
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
                    .then(
                        when {
                            hasTap -> {
                                Modifier.clickable(
                                    interactionSource = bubbleRipple.messageInteractionSource,
                                    indication = null,
                                    onClick = {},
                                )
                            }

                            else -> {
                                Modifier.conversationMessageNonTouchClickable(
                                    interactionSource = bubbleRipple.messageInteractionSource,
                                    onClickLabel = null,
                                    onClick = null,
                                    onLongClickLabel = null,
                                    onLongClick = { longPressCount++ },
                                )
                            }
                        },
                    ),
            ) {
                Spacer(modifier = Modifier.size(size = 100.dp))
                Spacer(
                    modifier = Modifier
                        .size(size = 100.dp)
                        .onPlaced { coordinates -> bubbleRipple.bubbleCoordinates = coordinates }
                        .then(
                            when {
                                hasTap -> Modifier

                                else -> {
                                    Modifier.conversationMessageBubbleClickable(
                                        interactionSource = bubbleRipple.bubbleInteractionSource,
                                        onClick = null,
                                        onLongClick = { longPressCount++ },
                                    )
                                }
                            },
                        ),
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
