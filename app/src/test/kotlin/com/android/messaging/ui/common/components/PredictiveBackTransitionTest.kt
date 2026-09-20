package com.android.messaging.ui.common.components

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.navigationevent.NavigationEvent
import kotlin.math.roundToLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictiveBackTransitionTest {

    @Test
    fun predictiveBackTranslation_pushesAwayFromTheSwipedEdge() {
        val fromLeft = translationFrom(NavigationEvent.EDGE_LEFT)
        val fromRight = translationFrom(NavigationEvent.EDGE_RIGHT)

        assertTrue("left swipe should push right, was $fromLeft", fromLeft > 0)
        assertEquals(-fromLeft, fromRight)
    }

    @Test
    fun predictiveBackTranslation_keepsTheScaledScreenInsideTheFarEdge() {
        val scaledHalfWidth = FULL_WIDTH * PREDICTIVE_BACK_TARGET_SCALE / 2f
        val farEdge = FULL_WIDTH / 2f + scaledHalfWidth + translationFrom(NavigationEvent.EDGE_LEFT)

        val expectedMargin = FULL_WIDTH * PREDICTIVE_BACK_EDGE_MARGIN_FRACTION
        assertEquals(FULL_WIDTH - expectedMargin, farEdge, 1f)
    }

    @Test
    fun predictiveBackTranslation_withoutAnEdgeOnlyShrinks() {
        assertEquals(0, translationFrom(NavigationEvent.EDGE_NONE))
    }

    /**
     * The gesture seeks the transition by play time, so a screen only follows the finger while
     * half the play time means half the movement. An eased spec would run ahead of the finger.
     */
    @Test
    fun predictiveBackSpec_movesEvenlyAcrossItsPlayTime() {
        val spec = predictiveBackSpec<Float>().vectorize(Float.VectorConverter)
        val start = AnimationVector1D(0f)
        val end = AnimationVector1D(1f)
        val durationNanos = spec.getDurationNanos(start, end, start)

        for (fraction in listOf(0.25f, 0.5f, 0.75f)) {
            val playTimeNanos = (fraction * durationNanos).roundToLong()
            val moved = spec.getValueFromNanos(playTimeNanos, start, end, start).value

            assertEquals("at $fraction of the play time", fraction, moved, TOLERANCE)
        }
    }

    private fun translationFrom(swipeEdge: Int): Int {
        return predictiveBackTranslation(swipeEdge = swipeEdge, fullWidth = FULL_WIDTH)
    }

    private companion object {
        const val FULL_WIDTH = 1080
        const val TOLERANCE = 0.01f
    }
}
