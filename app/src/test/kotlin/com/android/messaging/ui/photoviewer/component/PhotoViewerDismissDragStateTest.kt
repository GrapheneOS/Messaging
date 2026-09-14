package com.android.messaging.ui.photoviewer.component

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FLOAT_DELTA = 0.001f
private const val TEST_DISMISS_THRESHOLD = 200f

internal class PhotoViewerDismissDragStateTest {

    @Test
    fun canHandleDrag_allowsDownwardDragOnlyWhenImageIsNotZoomed() {
        val state = photoViewerDismissDragState()

        assertTrue(state.canHandleDrag(isZoomed = false, dragAmount = 1f))
        assertFalse(state.canHandleDrag(isZoomed = true, dragAmount = 1f))
        assertFalse(state.canHandleDrag(isZoomed = false, dragAmount = -1f))
    }

    @Test
    fun canHandleDrag_allowsUpwardDragAfterDismissDragStarts() {
        val state = photoViewerDismissDragState()

        state.applyDrag(dragAmount = 25f)

        assertTrue(state.canHandleDrag(isZoomed = false, dragAmount = -1f))
        assertFalse(state.canHandleDrag(isZoomed = true, dragAmount = -1f))
    }

    @Test
    fun applyDrag_clampsOffsetToZero() {
        val state = photoViewerDismissDragState()

        state.applyDrag(dragAmount = 25f)
        state.applyDrag(dragAmount = -50f)

        assertEquals(0f, state.animatedDragOffset, FLOAT_DELTA)
        assertFalse(state.canHandleDrag(isZoomed = false, dragAmount = -1f))
    }

    @Test
    fun shouldShowTopBar_flipsAtDismissProgressThreshold() {
        val state = photoViewerDismissDragState()

        state.applyDrag(dragAmount = 19f)

        assertTrue(state.shouldShowTopBar)

        state.applyDrag(dragAmount = 1f)

        assertFalse(state.shouldShowTopBar)
    }

    @Test
    fun backgroundAlpha_reachesZeroAtHalfDismissProgress() {
        val state = photoViewerDismissDragState()

        state.applyDrag(dragAmount = 50f)

        assertEquals(0.5f, state.backgroundAlpha, FLOAT_DELTA)

        state.applyDrag(dragAmount = 50f)

        assertEquals(0f, state.backgroundAlpha, FLOAT_DELTA)
    }

    @Test
    fun imageAlpha_clampsToMinimumAtFullDismissProgress() {
        val state = photoViewerDismissDragState()

        state.applyDrag(dragAmount = TEST_DISMISS_THRESHOLD * 2f)

        assertEquals(0.4f, state.imageAlpha, FLOAT_DELTA)
    }

    @Test
    fun shouldCloseOnDragEnd_usesHalfDismissThreshold() {
        val state = photoViewerDismissDragState()

        state.applyDrag(dragAmount = 99f)

        assertFalse(state.shouldCloseOnDragEnd())

        state.applyDrag(dragAmount = 1f)

        assertTrue(state.shouldCloseOnDragEnd())
    }

    @Test
    fun reset_clearsDismissOffsetAndCloseState() {
        val state = photoViewerDismissDragState()

        state.activate()
        state.applyDrag(dragAmount = 100f)

        state.reset()

        assertEquals(0f, state.animatedDragOffset, FLOAT_DELTA)
        assertFalse(state.shouldCloseOnDragEnd())
        assertTrue(state.shouldShowTopBar)
    }

    private fun photoViewerDismissDragState(): PhotoViewerDismissDragState {
        val dismissDragOffsetState = mutableFloatStateOf(value = 0f)

        return PhotoViewerDismissDragState(
            swipeDismissThreshold = TEST_DISMISS_THRESHOLD,
            dismissDragOffsetState = dismissDragOffsetState,
            isActiveState = mutableStateOf(value = false),
            animatedDismissDragOffsetState = object : State<Float> {
                override val value: Float
                    get() {
                        return dismissDragOffsetState.floatValue
                    }
            },
        )
    }
}
