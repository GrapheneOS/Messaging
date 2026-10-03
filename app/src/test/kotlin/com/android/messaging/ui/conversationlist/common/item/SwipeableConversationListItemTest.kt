package com.android.messaging.ui.conversationlist.common.item

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SwipeableConversationListItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun toggleReadSwipe_triggersOnlyOnceRowHasSettled() {
        var triggers = 0
        setContent(
            startToEndAction = ConversationSwipeAction(
                kind = ConversationSwipeKind.ToggleRead,
                onTrigger = { triggers += 1 },
            ),
        )

        swipeAndPauseMidSettle { swipeRight() }
        assertEquals(0, triggers)

        finishAnimations()
        assertEquals(1, triggers)
        composeTestRule
            .onNodeWithTag(testTag = CONTENT_TEST_TAG)
            .assertLeftPositionInRootIsEqualTo(expectedLeft = 0.dp)
    }

    @Test
    fun toggleReadSwipe_tapDuringSettle_rowStillSettlesAndTriggers() {
        var triggers = 0
        setContent(
            startToEndAction = ConversationSwipeAction(
                kind = ConversationSwipeKind.ToggleRead,
                onTrigger = { triggers += 1 },
            ),
        )

        swipeAndPauseMidSettle { swipeRight() }
        composeTestRule.onNodeWithTag(testTag = ROW_TEST_TAG).performTouchInput { click() }

        finishAnimations()
        assertEquals(1, triggers)
        composeTestRule
            .onNodeWithTag(testTag = CONTENT_TEST_TAG)
            .assertLeftPositionInRootIsEqualTo(expectedLeft = 0.dp)
    }

    @Test
    fun archiveSwipe_tapDuringSlide_stillArchives() {
        var triggers = 0
        setContent(
            endToStartAction = ConversationSwipeAction(
                kind = ConversationSwipeKind.Archive,
                onTrigger = { triggers += 1 },
            ),
        )

        swipeAndPauseMidSettle { swipeLeft() }
        composeTestRule.onNodeWithTag(testTag = ROW_TEST_TAG).performTouchInput { click() }

        finishAnimations()
        assertEquals(1, triggers)
    }

    private fun swipeAndPauseMidSettle(swipe: TouchInjectionScope.() -> Unit) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag(testTag = ROW_TEST_TAG).performTouchInput(block = swipe)
        composeTestRule.mainClock.advanceTimeByFrame()
    }

    private fun finishAnimations() {
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
    }

    private fun setContent(
        startToEndAction: ConversationSwipeAction? = null,
        endToStartAction: ConversationSwipeAction? = null,
    ) {
        composeTestRule.setContent {
            SwipeableConversationListItem(
                item = previewConversationListItem(
                    conversationId = ConversationId("swipe"),
                    title = "Bob",
                    snippetText = "Hello",
                ),
                isSelectionMode = false,
                isInteractionEnabled = true,
                appearanceAnimationToken = null,
                onAppearanceAnimationFinished = {},
                startToEndAction = startToEndAction,
                endToStartAction = endToStartAction,
                backgroundHorizontalInsets = PaddingValues(),
                modifier = Modifier.testTag(ROW_TEST_TAG),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .testTag(CONTENT_TEST_TAG),
                )
            }
        }
    }

    private companion object {
        const val ROW_TEST_TAG = "swipe_row"
        const val CONTENT_TEST_TAG = "swipe_row_content"
    }
}
