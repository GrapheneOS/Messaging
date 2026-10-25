package com.android.messaging.ui.common.components.selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SelectionListContentEndAnchorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val bottomPadding = mutableStateOf(value = KEYBOARD_PADDING)

    @Test
    fun reopeningTheKeyboardAtTheEnd_pushesTheLastItemBackAboveIt() {
        setListContent(itemCount = LONG_LIST_ITEM_COUNT)
        scrollToIndex(index = LONG_LIST_ITEM_COUNT - 1)

        animateBottomPadding(target = NAVIGATION_PADDING)
        animateBottomPadding(target = KEYBOARD_PADDING)

        assertItemBottom(
            index = LONG_LIST_ITEM_COUNT - 1,
            expected = HOST_HEIGHT - KEYBOARD_PADDING - LIST_END_SPACING,
        )
    }

    @Test
    fun reopeningTheKeyboardAtTheEnd_movesTheLastItemInTheSameFrameAsThePadding() {
        setListContent(itemCount = LONG_LIST_ITEM_COUNT)
        scrollToIndex(index = LONG_LIST_ITEM_COUNT - 1)
        animateBottomPadding(target = NAVIGATION_PADDING)
        composeTestRule.mainClock.autoAdvance = false

        for (step in 1..PADDING_ANIMATION_STEPS) {
            val padding = NAVIGATION_PADDING +
                (KEYBOARD_PADDING - NAVIGATION_PADDING) * step / PADDING_ANIMATION_STEPS
            bottomPadding.value = padding
            composeTestRule.waitForIdle()
            composeTestRule.mainClock.advanceTimeByFrame()

            assertPaddingProbeHeight(expected = padding)
            assertItemBottom(
                index = LONG_LIST_ITEM_COUNT - 1,
                expected = HOST_HEIGHT - padding - LIST_END_SPACING,
            )
        }
    }

    @Test
    fun reopeningTheKeyboardAtTheEndOfATallLastItem_pushesItsBottomBackAboveIt() {
        setListContent(itemCount = LONG_LIST_ITEM_COUNT, lastItemHeight = TALL_ITEM_HEIGHT)
        scrollToEnd()
        assertItemBottom(
            index = LONG_LIST_ITEM_COUNT - 1,
            expected = HOST_HEIGHT - KEYBOARD_PADDING - LIST_END_SPACING,
        )

        animateBottomPadding(target = NAVIGATION_PADDING)
        animateBottomPadding(target = KEYBOARD_PADDING)

        assertItemBottom(
            index = LONG_LIST_ITEM_COUNT - 1,
            expected = HOST_HEIGHT - KEYBOARD_PADDING - LIST_END_SPACING,
        )
    }

    @Test
    fun reopeningTheKeyboardAtTheEndOfAListThatFitsWithoutIt_pushesTheLastItemBackAboveIt() {
        setListContent(itemCount = SHORT_LIST_ITEM_COUNT)
        scrollToIndex(index = SHORT_LIST_ITEM_COUNT - 1)

        animateBottomPadding(target = NAVIGATION_PADDING)
        assertItemTop(index = 0, expected = 0.dp)
        animateBottomPadding(target = KEYBOARD_PADDING)

        assertItemBottom(
            index = SHORT_LIST_ITEM_COUNT - 1,
            expected = HOST_HEIGHT - KEYBOARD_PADDING - LIST_END_SPACING,
        )
    }

    @Test
    fun reopeningTheKeyboardAwayFromTheEnd_keepsTheScrollPosition() {
        setListContent(itemCount = LONG_LIST_ITEM_COUNT)
        scrollToIndex(index = MIDDLE_ITEM_INDEX)

        animateBottomPadding(target = NAVIGATION_PADDING)
        animateBottomPadding(target = KEYBOARD_PADDING)

        assertItemTop(index = MIDDLE_ITEM_INDEX, expected = 0.dp)
    }

    @Test
    fun openingTheKeyboardOverAnUnscrolledListThatFits_keepsTheTop() {
        bottomPadding.value = NAVIGATION_PADDING
        setListContent(itemCount = SHORT_LIST_ITEM_COUNT)

        animateBottomPadding(target = KEYBOARD_PADDING)

        assertItemTop(index = 0, expected = 0.dp)
    }

    private fun setListContent(itemCount: Int, lastItemHeight: Dp = ITEM_HEIGHT) {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(width = HOST_WIDTH, height = HOST_HEIGHT)) {
                SelectionListContent(
                    modifier = Modifier.fillMaxSize(),
                    canLoadMore = false,
                    isLoading = false,
                    isLoadingMore = false,
                    loadMoreItemCount = itemCount,
                    onLoadMore = {},
                    contentPadding = PaddingValues(bottom = bottomPadding.value),
                ) {
                    items(count = itemCount) { index ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    height = when (index) {
                                        itemCount - 1 -> lastItemHeight
                                        else -> ITEM_HEIGHT
                                    },
                                )
                                .testTag(itemTag(index = index)),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(bottomPadding.value)
                        .testTag(PADDING_PROBE_TAG),
                )
            }
        }
    }

    private fun scrollToIndex(index: Int) {
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
        composeTestRule.waitForIdle()
    }

    private fun scrollToEnd() {
        composeTestRule
            .onNode(hasScrollToIndexAction())
            .performSemanticsAction(key = SemanticsActions.ScrollBy) { scrollBy ->
                scrollBy(0f, SCROLL_TO_END_DISTANCE_PX)
            }
        composeTestRule.waitForIdle()
    }

    private fun animateBottomPadding(target: Dp) {
        val start = bottomPadding.value

        for (step in 1..PADDING_ANIMATION_STEPS) {
            bottomPadding.value = start + (target - start) * step / PADDING_ANIMATION_STEPS
            composeTestRule.waitForIdle()
        }
    }

    private fun assertPaddingProbeHeight(expected: Dp) {
        val bounds = composeTestRule.onNodeWithTag(PADDING_PROBE_TAG).getBoundsInRoot()

        assertEquals(expected.value, (bounds.bottom - bounds.top).value, POSITION_TOLERANCE_DP)
    }

    private fun assertItemTop(index: Int, expected: Dp) {
        val bounds = composeTestRule.onNodeWithTag(itemTag(index = index)).getBoundsInRoot()

        assertEquals(expected.value, bounds.top.value, POSITION_TOLERANCE_DP)
    }

    private fun assertItemBottom(index: Int, expected: Dp) {
        val bounds = composeTestRule.onNodeWithTag(itemTag(index = index)).getBoundsInRoot()

        assertEquals(expected.value, bounds.bottom.value, POSITION_TOLERANCE_DP)
    }

    private fun itemTag(index: Int): String {
        return "item_$index"
    }

    private companion object {
        private const val PADDING_PROBE_TAG = "padding_probe"
        private const val LONG_LIST_ITEM_COUNT = 40
        private const val SHORT_LIST_ITEM_COUNT = 6
        private const val MIDDLE_ITEM_INDEX = 10
        private const val PADDING_ANIMATION_STEPS = 8
        private const val POSITION_TOLERANCE_DP = 1f
        private const val SCROLL_TO_END_DISTANCE_PX = 100_000f

        private val HOST_WIDTH = 320.dp
        private val HOST_HEIGHT = 400.dp
        private val ITEM_HEIGHT = 50.dp
        private val TALL_ITEM_HEIGHT = 300.dp
        private val NAVIGATION_PADDING = 24.dp
        private val KEYBOARD_PADDING = 200.dp
        private val LIST_END_SPACING = 16.dp
    }
}
