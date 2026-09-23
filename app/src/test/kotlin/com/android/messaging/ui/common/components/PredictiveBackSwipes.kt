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

internal class PredictiveBackSwipes(
    private val composeTestRule: AndroidComposeTestRule<*, ComponentActivity>,
    private val closingPageTag: String,
) {
    fun start(progress: Float = PROGRESS) {
        dispatch { onBackPressedDispatcher.dispatchOnBackStarted(backEvent()) }
        dispatch {
            onBackPressedDispatcher.dispatchOnBackProgressed(backEvent(progress = progress))
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

        return Page(scale = node.boundsInRoot.width / node.size.width)
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

    private fun backEvent(progress: Float = 0f): BackEventCompat {
        return BackEventCompat(
            touchX = 0f,
            touchY = 0f,
            progress = progress,
            swipeEdge = BackEventCompat.EDGE_LEFT,
        )
    }

    data class Page(
        val scale: Float,
    )

    companion object {
        const val PROGRESS = 0.5f
        const val PIXEL_TOLERANCE = 1f
        private const val ANIMATION_FRAMES = 40
    }
}
