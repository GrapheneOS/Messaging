package com.android.messaging.ui.common.components

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

internal class PredictiveBackSwipes(
    private val composeTestRule: AndroidComposeTestRule<*, ComponentActivity>,
    private val closingPageTag: String,
) {
    val pageHeight: Float
        get() = checkNotNull(page(tag = closingPageTag)).height

    private var startY = START_Y

    fun start(touchDeltaY: Float, startY: Float = START_Y, progress: Float = PROGRESS) {
        this.startY = startY
        dispatch { onBackPressedDispatcher.dispatchOnBackStarted(backEvent(touchY = startY)) }
        moveFinger(touchDeltaY = touchDeltaY, progress = progress)
    }

    fun moveFinger(touchDeltaY: Float, progress: Float = PROGRESS) {
        dispatch {
            onBackPressedDispatcher.dispatchOnBackProgressed(
                backEvent(touchY = startY + touchDeltaY, progress = progress),
            )
        }
    }

    // With the clock stopped, this only lets the change reach composition.
    fun dispatch(block: ComponentActivity.() -> Unit) {
        composeTestRule.runOnUiThread { composeTestRule.activity.block() }
        composeTestRule.waitForIdle()
    }

    /** Samples the closing page each frame while it's there, calling [onFrame] after each frame. */
    fun advanceFrames(count: Int = ANIMATION_FRAMES, onFrame: () -> Unit = {}): List<Page> {
        return List(count) {
            composeTestRule.mainClock.advanceTimeByFrame()
            onFrame()
            page(tag = closingPageTag)
        }.filterNotNull()
    }

    fun page(tag: String = closingPageTag): Page? {
        val node = composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().singleOrNull()
            ?: return null
        val bounds = node.boundsInRoot

        return Page(
            shift = bounds.center.y - node.size.height / 2f,
            scale = bounds.width / node.size.width,
            height = node.size.height.toFloat(),
            edgeMarginPx = with(composeTestRule.density) { EDGE_MARGIN.toPx() },
        )
    }

    fun bounds(tag: String): Rect? {
        return composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().singleOrNull()
            ?.boundsInRoot
    }

    /** For a closing page that draws red over a revealed page that draws none. */
    fun closingPageAlpha(): Float {
        val center = checkNotNull(bounds(closingPageTag)).center

        return pixel(x = center.x, y = center.y).red
    }

    fun pixel(x: Float, y: Float): Color {
        return composeTestRule.onRoot().captureToImage().toPixelMap()[x.toInt(), y.toInt()]
    }

    private fun backEvent(touchY: Float, progress: Float = 0f): BackEventCompat {
        return BackEventCompat(
            touchX = 0f,
            touchY = touchY,
            progress = progress,
            swipeEdge = BackEventCompat.EDGE_LEFT,
        )
    }

    data class Page(
        val shift: Float,
        val scale: Float,
        val height: Float,
        val edgeMarginPx: Float,
    ) {
        /** The shift for a finger moved by [touchDeltaY], at the scale the page is drawn at. */
        fun expectedShift(touchDeltaY: Float): Float {
            return predictiveBackVerticalShift(
                touchDeltaY = touchDeltaY,
                scale = scale,
                height = height,
                edgeMarginPx = edgeMarginPx,
            )
        }
    }

    companion object {
        const val PROGRESS = 0.5f
        const val PIXEL_TOLERANCE = 1f
        const val MIN_EXPECTED_SHIFT = 5f
        const val START_Y = 200f
        private const val ANIMATION_FRAMES = 40
        private const val MIN_EASING_FRAMES = 3
        private val EDGE_MARGIN = 8.dp

        fun assertFollows(touchDeltaY: Float, page: Page?) {
            val expected = checkNotNull(page).expectedShift(touchDeltaY = touchDeltaY)

            // Guards against a screen too small for any shift, which would pass trivially.
            assertTrue("expected shift $expected", abs(expected) > MIN_EXPECTED_SHIFT)
            assertEquals("moved by $touchDeltaY", expected, page.shift, PIXEL_TOLERANCE)
        }

        /** Checks the page eases back to the middle with the gesture that moved it by [touchDeltaY]. */
        fun assertEasesBack(touchDeltaY: Float, frames: List<Page>) {
            for (frame in frames) {
                assertEquals(
                    "frames $frames",
                    frame.expectedShift(touchDeltaY = touchDeltaY),
                    frame.shift,
                    PIXEL_TOLERANCE,
                )
            }
            for ((previous, next) in frames.zipWithNext()) {
                assertTrue("frames $frames", next.shift <= previous.shift + PIXEL_TOLERANCE)
            }
            val firstShift = frames.first().shift
            val easingFrames = frames.count { it.shift in MIN_EXPECTED_SHIFT..firstShift - 1f }
            assertTrue("frames $frames", easingFrames >= MIN_EASING_FRAMES)
            assertEquals(0f, frames.last().shift, PIXEL_TOLERANCE)
        }
    }
}
