package com.android.messaging.ui.photoviewer.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotoViewerTapInputTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val displayMode = mutableStateOf(value = PhotoViewerDisplayMode.Carousel)
    private val gestureState = ZoomablePhotoGestureState()
    private var toggleCount = 0
    private var enterImmersiveCount = 0

    @Test
    fun tap_inCarousel_entersImmersiveWithoutWaitingForDoubleTapTimeout() {
        setContent(initialDisplayMode = PhotoViewerDisplayMode.Carousel)

        performTouchInput { click() }

        assertEquals(1, toggleCount)
        assertEquals(PhotoViewerDisplayMode.Immersive, displayMode.value)
    }

    @Test
    fun tap_inImmersive_leavesOnlyAfterDoubleTapTimeout() {
        setContent(initialDisplayMode = PhotoViewerDisplayMode.Immersive)

        performTouchInput { click() }
        composeTestRule.mainClock.advanceTimeBy(milliseconds = BEFORE_DOUBLE_TAP_TIMEOUT_MILLIS)
        assertEquals(0, toggleCount)

        composeTestRule.mainClock.advanceTimeBy(milliseconds = PAST_DOUBLE_TAP_TIMEOUT_MILLIS)
        assertEquals(1, toggleCount)
        assertEquals(PhotoViewerDisplayMode.Carousel, displayMode.value)
    }

    @Test
    fun doubleTap_inCarousel_entersImmersiveOnceAndZooms() {
        setContent(initialDisplayMode = PhotoViewerDisplayMode.Carousel)

        performTouchInput { doubleClick() }
        composeTestRule.mainClock.advanceTimeBy(milliseconds = PAST_DOUBLE_TAP_TIMEOUT_MILLIS)

        assertEquals(1, toggleCount)
        assertEquals(1, enterImmersiveCount)
        assertEquals(PhotoViewerDisplayMode.Immersive, displayMode.value)
        assertTrue(gestureState.isZoomed())
    }

    @Test
    fun doubleTap_inImmersive_zoomsWithoutLeavingImmersive() {
        setContent(initialDisplayMode = PhotoViewerDisplayMode.Immersive)

        performTouchInput { doubleClick() }
        composeTestRule.mainClock.advanceTimeBy(milliseconds = PAST_DOUBLE_TAP_TIMEOUT_MILLIS)

        assertEquals(0, toggleCount)
        assertEquals(1, enterImmersiveCount)
        assertEquals(PhotoViewerDisplayMode.Immersive, displayMode.value)
        assertTrue(gestureState.isZoomed())
    }

    @Test
    fun doubleTap_inImmersiveWhileZoomed_zoomsOutWithoutLeavingImmersive() {
        setContent(initialDisplayMode = PhotoViewerDisplayMode.Immersive)

        performTouchInput { doubleClick() }
        composeTestRule.mainClock.advanceTimeBy(milliseconds = PAST_DOUBLE_TAP_TIMEOUT_MILLIS)
        performTouchInput { doubleClick() }
        composeTestRule.mainClock.advanceTimeBy(milliseconds = PAST_DOUBLE_TAP_TIMEOUT_MILLIS)

        assertEquals(0, toggleCount)
        assertEquals(PhotoViewerDisplayMode.Immersive, displayMode.value)
        assertFalse(gestureState.isZoomed())
    }

    private fun performTouchInput(block: TouchInjectionScope.() -> Unit) {
        composeTestRule
            .onNodeWithTag(testTag = TAP_TARGET_TEST_TAG)
            .performTouchInput(block = block)
    }

    private fun setContent(initialDisplayMode: PhotoViewerDisplayMode) {
        displayMode.value = initialDisplayMode
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(size = 200.dp)
                    .testTag(tag = TAP_TARGET_TEST_TAG)
                    .photoViewerTapInput(
                        contentKey = PhotoViewerContentKey(
                            page = 0,
                            contentUri = "content://photo",
                        ),
                        gestureState = gestureState,
                        currentDisplayMode = displayMode,
                        currentOnToggleDisplayMode = mutableStateOf<() -> Unit>(
                            value = { toggleDisplayMode() },
                        ),
                        currentOnEnterImmersiveMode = mutableStateOf<() -> Unit>(
                            value = { enterImmersiveMode() },
                        ),
                    ),
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false
    }

    private fun toggleDisplayMode() {
        toggleCount += 1
        displayMode.value = when (displayMode.value) {
            PhotoViewerDisplayMode.Carousel -> PhotoViewerDisplayMode.Immersive
            PhotoViewerDisplayMode.Immersive -> PhotoViewerDisplayMode.Carousel
        }
    }

    private fun enterImmersiveMode() {
        enterImmersiveCount += 1
        displayMode.value = PhotoViewerDisplayMode.Immersive
    }

    private companion object {
        const val TAP_TARGET_TEST_TAG = "photo_viewer_tap_target"
        const val BEFORE_DOUBLE_TAP_TIMEOUT_MILLIS = 200L
        const val PAST_DOUBLE_TAP_TIMEOUT_MILLIS = 400L
    }
}
