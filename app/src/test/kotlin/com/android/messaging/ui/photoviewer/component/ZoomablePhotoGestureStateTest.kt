package com.android.messaging.ui.photoviewer.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FLOAT_DELTA = 0.001f

internal class ZoomablePhotoGestureStateTest {

    @Test
    fun initialState_isUnzoomedAndUntranslated() {
        val state = ZoomablePhotoGestureState()

        assertEquals(IntSize.Zero, state.containerSize)
        assertEquals(1f, state.scale, FLOAT_DELTA)
        assertEquals(Offset.Zero, state.offset)
        assertFalse(state.isZoomed())
        assertFalse(state.isZoomAnimationEnabled)
    }

    @Test
    fun applyTransform_clampsScaleToSupportedRange() {
        val state = ZoomablePhotoGestureState()

        state.updateContainerSize(containerSize = IntSize(width = 100, height = 100))

        assertTrue(state.applyTransform(zoomChange = 10f, panChange = Offset.Zero))
        assertEquals(5f, state.scale, FLOAT_DELTA)
        assertTrue(state.isZoomed())

        assertTrue(state.applyTransform(zoomChange = 0.1f, panChange = Offset(x = 50f, y = 50f)))
        assertEquals(1f, state.scale, FLOAT_DELTA)
        assertEquals(Offset.Zero, state.offset)
        assertFalse(state.isZoomed())
        assertFalse(state.isZoomAnimationEnabled)
    }

    @Test
    fun applyTransform_clampsPanOffsetToScaledContainerBounds() {
        val state = ZoomablePhotoGestureState()

        state.updateContainerSize(containerSize = IntSize(width = 100, height = 200))
        state.applyTransform(zoomChange = 3f, panChange = Offset.Zero)

        assertFalse(state.applyTransform(zoomChange = 1f, panChange = Offset(x = 250f, y = -250f)))
        assertEquals(Offset(x = 100f, y = -200f), state.offset)
    }

    @Test
    fun applyTransform_withCentroid_keepsThePointUnderTheFingersInPlace() {
        val state = ZoomablePhotoGestureState()

        state.updateContainerSize(containerSize = IntSize(width = 100, height = 100))
        state.applyTransform(
            zoomChange = 2f,
            panChange = Offset.Zero,
            centroid = Offset(x = 75f, y = 25f),
        )

        assertEquals(2f, state.scale, FLOAT_DELTA)
        assertEquals(Offset(x = -25f, y = 25f), state.offset)
        assertScreenPositionOfContentPoint(
            state = state,
            contentPoint = Offset(x = 75f, y = 25f),
            expectedScreenPosition = Offset(x = 75f, y = 25f),
        )
    }

    @Test
    fun applyTransform_withCentroidWhileZoomed_keepsThePointUnderTheFingersInPlace() {
        val state = ZoomablePhotoGestureState()

        state.updateContainerSize(containerSize = IntSize(width = 100, height = 100))
        state.applyTransform(
            zoomChange = 2f,
            panChange = Offset.Zero,
            centroid = Offset(x = 75f, y = 25f),
        )
        val contentPointUnderFingers = Offset(x = 55f, y = 60f)
        val fingers = screenPositionOfContentPoint(
            state = state,
            contentPoint = contentPointUnderFingers,
        )

        state.applyTransform(zoomChange = 1.5f, panChange = Offset.Zero, centroid = fingers)

        assertEquals(3f, state.scale, FLOAT_DELTA)
        assertScreenPositionOfContentPoint(
            state = state,
            contentPoint = contentPointUnderFingers,
            expectedScreenPosition = fingers,
        )
    }

    @Test
    fun applyTransform_withMovingCentroid_movesThePointUnderTheFingersAlong() {
        val state = ZoomablePhotoGestureState()

        state.updateContainerSize(containerSize = IntSize(width = 100, height = 100))
        state.applyTransform(zoomChange = 2f, panChange = Offset.Zero)

        state.applyTransform(
            zoomChange = 1.25f,
            panChange = Offset(x = 4f, y = -6f),
            centroid = Offset(x = 40f, y = 60f),
        )

        assertScreenPositionOfContentPoint(
            state = state,
            contentPoint = Offset(x = 45f, y = 55f),
            expectedScreenPosition = Offset(x = 44f, y = 54f),
        )
    }

    @Test
    fun applyTransform_withCentroidAtTheCorner_keepsTheCornerInPlaceWithinBounds() {
        val state = ZoomablePhotoGestureState()

        state.updateContainerSize(containerSize = IntSize(width = 100, height = 100))
        state.applyTransform(zoomChange = 2f, panChange = Offset.Zero, centroid = Offset.Zero)
        state.applyTransform(zoomChange = 0.75f, panChange = Offset.Zero, centroid = Offset.Zero)

        assertEquals(1.5f, state.scale, FLOAT_DELTA)
        assertEquals(Offset(x = 25f, y = 25f), state.offset)
        assertScreenPositionOfContentPoint(
            state = state,
            contentPoint = Offset.Zero,
            expectedScreenPosition = Offset.Zero,
        )
    }

    @Test
    fun applyTransform_whenTheAnchoredOffsetLeavesTheImage_clampsItToTheEdge() {
        val state = ZoomablePhotoGestureState()

        state.updateContainerSize(containerSize = IntSize(width = 100, height = 100))
        state.applyTransform(zoomChange = 3f, panChange = Offset.Zero, centroid = Offset.Zero)
        state.applyTransform(
            zoomChange = 1f,
            panChange = Offset(x = 80f, y = 80f),
            centroid = Offset(x = 20f, y = 20f),
        )

        assertEquals(Offset(x = 100f, y = 100f), state.offset)
    }

    @Test
    fun updateContainerSize_reclampsExistingOffset() {
        val state = ZoomablePhotoGestureState()

        state.updateContainerSize(containerSize = IntSize(width = 200, height = 200))
        state.applyTransform(zoomChange = 3f, panChange = Offset.Zero)
        state.applyTransform(zoomChange = 1f, panChange = Offset(x = 300f, y = 300f))

        assertEquals(Offset(x = 200f, y = 200f), state.offset)

        state.updateContainerSize(containerSize = IntSize(width = 100, height = 100))

        assertEquals(Offset(x = 100f, y = 100f), state.offset)
    }

    @Test
    fun applyDoubleTap_whenUnzoomed_zoomsAroundTapOffset() {
        val state = ZoomablePhotoGestureState()

        state.updateContainerSize(containerSize = IntSize(width = 100, height = 100))
        state.applyDoubleTap(tapOffset = Offset(x = 75f, y = 25f))

        assertEquals(2.5f, state.scale, FLOAT_DELTA)
        assertEquals(Offset(x = -37.5f, y = 37.5f), state.offset)
        assertTrue(state.isZoomed())
        assertTrue(state.isZoomAnimationEnabled)
    }

    @Test
    fun applyDoubleTap_whenZoomed_resetsZoomAndOffset() {
        val state = ZoomablePhotoGestureState()

        state.updateContainerSize(containerSize = IntSize(width = 100, height = 100))
        state.applyDoubleTap(tapOffset = Offset(x = 75f, y = 25f))
        state.applyDoubleTap(tapOffset = Offset(x = 50f, y = 50f))

        assertEquals(1f, state.scale, FLOAT_DELTA)
        assertEquals(Offset.Zero, state.offset)
        assertFalse(state.isZoomed())
        assertTrue(state.isZoomAnimationEnabled)
    }

    @Test
    fun resetAll_clearsZoomOffsetAndAnimationFlag() {
        val state = ZoomablePhotoGestureState()

        state.updateContainerSize(containerSize = IntSize(width = 100, height = 100))
        state.applyDoubleTap(tapOffset = Offset(x = 75f, y = 25f))

        state.resetAll()

        assertEquals(1f, state.scale, FLOAT_DELTA)
        assertEquals(Offset.Zero, state.offset)
        assertFalse(state.isZoomed())
        assertFalse(state.isZoomAnimationEnabled)
    }

    @Test
    fun isZoomed_usesEpsilonAboveDefaultScale() {
        val state = ZoomablePhotoGestureState()

        state.updateContainerSize(containerSize = IntSize(width = 100, height = 100))
        state.applyTransform(zoomChange = 1.005f, panChange = Offset.Zero)

        assertFalse(state.isZoomed())

        state.applyTransform(zoomChange = 1.02f, panChange = Offset.Zero)

        assertTrue(state.isZoomed())
    }

    /**
     * Where a point of the unzoomed content ends up on screen: the content is scaled around the
     * container center and then translated by the offset, as the photo viewer's graphics layer does.
     */
    private fun screenPositionOfContentPoint(
        state: ZoomablePhotoGestureState,
        contentPoint: Offset,
    ): Offset {
        val containerCenter = Offset(
            x = state.containerSize.width / 2f,
            y = state.containerSize.height / 2f,
        )

        return containerCenter + state.offset + (contentPoint - containerCenter) * state.scale
    }

    private fun assertScreenPositionOfContentPoint(
        state: ZoomablePhotoGestureState,
        contentPoint: Offset,
        expectedScreenPosition: Offset,
    ) {
        val actualScreenPosition = screenPositionOfContentPoint(
            state = state,
            contentPoint = contentPoint,
        )

        assertEquals(expectedScreenPosition.x, actualScreenPosition.x, FLOAT_DELTA)
        assertEquals(expectedScreenPosition.y, actualScreenPosition.y, FLOAT_DELTA)
    }
}
