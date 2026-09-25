package com.android.messaging.ui.common.components

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.navigationevent.NavigationEvent
import kotlin.math.roundToLong
import org.junit.Assert.assertEquals
import org.junit.Test

class PredictiveBackTransitionTest {

    @Test
    fun predictiveBackTranslation_fromTheRightEdgeOnlyShrinks() {
        assertEquals(0, translationFrom(NavigationEvent.EDGE_RIGHT))
    }

    @Test
    fun predictiveBackTranslation_otherwiseLeavesTheMarginAtTheRightEdge() {
        val scaledHalfWidth = FULL_WIDTH * PREDICTIVE_BACK_TARGET_SCALE / 2f

        for (swipeEdge in listOf(NavigationEvent.EDGE_LEFT, NavigationEvent.EDGE_NONE)) {
            val rightEdge = FULL_WIDTH / 2f + scaledHalfWidth + translationFrom(swipeEdge)

            assertEquals("edge $swipeEdge", (FULL_WIDTH - EDGE_MARGIN_PX).toFloat(), rightEdge, 1f)
        }
    }

    @Test
    fun predictiveBackTranslation_neverPullsANarrowScreenLeft() {
        val translation = predictiveBackTranslation(
            swipeEdge = NavigationEvent.EDGE_LEFT,
            fullWidth = EDGE_MARGIN_PX,
            edgeMarginPx = EDGE_MARGIN_PX,
        )

        assertEquals(0, translation)
    }

    @Test
    fun predictiveBackGestureSpec_followsThePlatformGestureCurve() {
        val spec = predictiveBackGestureSpec<Float>()

        // PathInterpolator(0.1, 0.1, 0, 1), solved numerically
        val expectedValues = mapOf(0.25f to 0.6838f, 0.5f to 0.8949f, 0.75f to 0.9783f)
        for ((fraction, expected) in expectedValues) {
            assertEquals(
                "at $fraction of the play time",
                expected,
                spec.valueAt(fraction),
                TOLERANCE,
            )
        }
    }

    @Test
    fun predictiveBackReleaseAlpha_holdsUntilTheCommit() {
        for (progress in listOf(0f, 0.5f, 1f)) {
            assertEquals("at $progress", 1f, releaseAlpha(progress, Float.NaN), TOLERANCE)
        }
    }

    @Test
    fun predictiveBackReleaseAlpha_fadesOverWhatIsLeftAfterTheRelease() {
        assertEquals(1f, releaseAlpha(progress = 0.3f, releaseProgress = 0.3f), TOLERANCE)
        assertEquals(
            1f - FastOutSlowInEasing.transform(0.5f),
            releaseAlpha(progress = 0.65f, releaseProgress = 0.3f),
            TOLERANCE,
        )
        assertEquals(0f, releaseAlpha(progress = 1f, releaseProgress = 0.3f), TOLERANCE)
    }

    // Nothing is left to fade over; the transition ends as the swipe is released.
    @Test
    fun predictiveBackReleaseAlpha_releasedAtTheEndStaysOpaque() {
        assertEquals(1f, releaseAlpha(progress = 1f, releaseProgress = 1f), TOLERANCE)
    }

    @Test
    fun predictiveBackVerticalShift_atFullDragStopsTheMarginFromTheScreenEdge() {
        val scaledHalfHeight = FULL_HEIGHT * PREDICTIVE_BACK_TARGET_SCALE / 2f

        for (touchDeltaY in listOf(FULL_HEIGHT / 2f, FULL_HEIGHT.toFloat())) {
            val bottomEdge = FULL_HEIGHT / 2f + scaledHalfHeight + verticalShift(touchDeltaY)
            val topEdge = FULL_HEIGHT / 2f - scaledHalfHeight + verticalShift(-touchDeltaY)

            assertEquals((FULL_HEIGHT - EDGE_MARGIN_PX).toFloat(), bottomEdge, TOLERANCE)
            assertEquals(EDGE_MARGIN_PX.toFloat(), topEdge, TOLERANCE)
        }
    }

    @Test
    fun predictiveBackVerticalShift_deceleratesWithFingerTravel() {
        val fullShift = verticalShift(touchDeltaY = FULL_HEIGHT / 2f)

        // A quarter of the height is half the travel: 1 - (1 - 0.5)^2
        assertEquals(0.75f * fullShift, verticalShift(touchDeltaY = FULL_HEIGHT / 4f), TOLERANCE)
    }

    @Test
    fun predictiveBackVerticalShift_staysPutWithoutTravelOrRoom() {
        assertEquals(0f, verticalShift(touchDeltaY = 0f), TOLERANCE)
        assertEquals(0f, verticalShift(touchDeltaY = FULL_HEIGHT / 2f, scale = 1f), TOLERANCE)
    }

    private fun FiniteAnimationSpec<Float>.valueAt(fraction: Float): Float {
        val vectorized = vectorize(Float.VectorConverter)
        val start = AnimationVector1D(0f)
        val end = AnimationVector1D(1f)
        val playTimeNanos =
            (fraction * vectorized.getDurationNanos(start, end, start)).roundToLong()

        return vectorized.getValueFromNanos(playTimeNanos, start, end, start).value
    }

    private fun releaseAlpha(progress: Float, releaseProgress: Float): Float {
        return predictiveBackReleaseAlpha(progress = progress, releaseProgress = releaseProgress)
    }

    private fun translationFrom(swipeEdge: Int): Int {
        return predictiveBackTranslation(
            swipeEdge = swipeEdge,
            fullWidth = FULL_WIDTH,
            edgeMarginPx = EDGE_MARGIN_PX,
        )
    }

    private fun verticalShift(
        touchDeltaY: Float,
        scale: Float = PREDICTIVE_BACK_TARGET_SCALE,
    ): Float {
        return predictiveBackVerticalShift(
            touchDeltaY = touchDeltaY,
            scale = scale,
            height = FULL_HEIGHT.toFloat(),
            edgeMarginPx = EDGE_MARGIN_PX.toFloat(),
        )
    }

    private companion object {
        const val FULL_WIDTH = 1080
        const val FULL_HEIGHT = 2340
        const val EDGE_MARGIN_PX = 21
        const val TOLERANCE = 0.01f
    }
}
