package com.android.messaging.ui.photoviewer.component

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FLOAT_DELTA = 0.001f
private const val TEST_TOUCH_SLOP = 18f

internal class PhotoViewerGestureDecisionTest {

    @Test
    fun shouldStartPhotoViewerTransform_whenPinchMotionExceedsTouchSlop_returnsTrue() {
        val shouldStart = shouldStartPhotoViewerTransform(
            isZoomed = false,
            accumulatedZoom = 1.2f,
            centroidSize = 100f,
            accumulatedPan = Offset.Zero,
            touchSlop = TEST_TOUCH_SLOP,
            pressedPointerCount = 2,
        )

        assertTrue(shouldStart)
    }

    @Test
    fun shouldStartPhotoViewerTransform_whenPinchMotionIsBelowTouchSlop_returnsFalse() {
        val shouldStart = shouldStartPhotoViewerTransform(
            isZoomed = false,
            accumulatedZoom = 1.1f,
            centroidSize = 100f,
            accumulatedPan = Offset.Zero,
            touchSlop = TEST_TOUCH_SLOP,
            pressedPointerCount = 2,
        )

        assertFalse(shouldStart)
    }

    @Test
    fun shouldStartPhotoViewerTransform_whenZoomedPanExceedsTouchSlop_returnsTrue() {
        val shouldStart = shouldStartPhotoViewerTransform(
            isZoomed = true,
            accumulatedZoom = 1f,
            centroidSize = 0f,
            accumulatedPan = Offset(x = TEST_TOUCH_SLOP + 1f, y = 0f),
            touchSlop = TEST_TOUCH_SLOP,
            pressedPointerCount = 1,
        )

        assertTrue(shouldStart)
    }

    @Test
    fun shouldStartPhotoViewerTransform_whenUnzoomedSinglePointerPans_returnsFalse() {
        val shouldStart = shouldStartPhotoViewerTransform(
            isZoomed = false,
            accumulatedZoom = 1f,
            centroidSize = 0f,
            accumulatedPan = Offset(x = TEST_TOUCH_SLOP + 1f, y = 0f),
            touchSlop = TEST_TOUCH_SLOP,
            pressedPointerCount = 1,
        )

        assertFalse(shouldStart)
    }

    @Test
    fun resolvePhotoViewerDismissDragDecision_whenDownwardDragStarts_appliesSlopSubtraction() {
        val decision = resolvePhotoViewerDismissDragDecision(
            canHandleDismissDrag = true,
            accumulatedDrag = Offset(x = 4f, y = 30f),
            dragAmount = 30f,
            isDismissDragActive = false,
            touchSlop = TEST_TOUCH_SLOP,
        )

        assertTrue(decision.isActive)
        assertEquals(12f, decision.dragAmount, FLOAT_DELTA)
    }

    @Test
    fun resolvePhotoViewerDismissDragDecision_whenDismissDragIsActive_appliesPerEventDelta() {
        val decision = resolvePhotoViewerDismissDragDecision(
            canHandleDismissDrag = true,
            accumulatedDrag = Offset(x = 0f, y = 200f),
            dragAmount = -8f,
            isDismissDragActive = true,
            touchSlop = TEST_TOUCH_SLOP,
        )

        assertTrue(decision.isActive)
        assertEquals(-8f, decision.dragAmount, FLOAT_DELTA)
    }

    @Test
    fun resolvePhotoViewerDismissDragDecision_whenImageIsZoomed_returnsInactiveDecision() {
        val decision = resolvePhotoViewerDismissDragDecision(
            canHandleDismissDrag = false,
            accumulatedDrag = Offset(x = 0f, y = TEST_TOUCH_SLOP + 20f),
            dragAmount = TEST_TOUCH_SLOP + 20f,
            isDismissDragActive = false,
            touchSlop = TEST_TOUCH_SLOP,
        )

        assertFalse(decision.isActive)
    }

    @Test
    fun shouldStartPhotoViewerDismissDrag_whenHorizontalDragDominates_returnsFalse() {
        val shouldStart = shouldStartPhotoViewerDismissDrag(
            accumulatedDrag = Offset(x = TEST_TOUCH_SLOP + 20f, y = TEST_TOUCH_SLOP + 1f),
            touchSlop = TEST_TOUCH_SLOP,
        )

        assertFalse(shouldStart)
    }

    @Test
    fun shouldStartPhotoViewerDismissDrag_whenDragMovesUp_returnsFalse() {
        val shouldStart = shouldStartPhotoViewerDismissDrag(
            accumulatedDrag = Offset(x = 0f, y = -TEST_TOUCH_SLOP - 20f),
            touchSlop = TEST_TOUCH_SLOP,
        )

        assertFalse(shouldStart)
    }
}
