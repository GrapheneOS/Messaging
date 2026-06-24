package com.android.messaging.ui.common.components.reorder

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayReorderGeometryTest {

    private val geometry = OverlayReorderGeometry()

    @Test
    fun isAcceptableTarget_notCommitted_isRejected() {
        assertFalse(isAcceptableTarget(isCommitted = false))
    }

    @Test
    fun isAcceptableTarget_alreadyStarted_isRejected() {
        assertFalse(isAcceptableTarget(isStarted = true))
    }

    @Test
    fun isAcceptableTarget_notPhysicallyVisible_isRejected() {
        assertFalse(isAcceptableTarget(isPhysicallyVisible = false))
    }

    @Test
    fun isAcceptableTarget_notLogicallyVisible_isRejected() {
        assertFalse(isAcceptableTarget(isLogicallyVisible = false))
    }

    @Test
    fun isAcceptableTarget_modelNotYetSettled_isRejected() {
        assertFalse(isAcceptableTarget(isModelSettled = false))
    }

    @Test
    fun isAcceptableTarget_movesUpToHigherPosition_isAccepted() {
        assertTrue(
            isAcceptableTarget(
                sourceIndex = 3,
                sourceTop = 300f,
                candidateTop = 0f,
                targetIndex = 0,
            ),
        )
    }

    @Test
    fun isAcceptableTarget_movesUpButCandidateNotAboveSource_isRejected() {
        assertFalse(
            isAcceptableTarget(
                sourceIndex = 3,
                sourceTop = 300f,
                candidateTop = 300f,
                targetIndex = 0,
            ),
        )
    }

    @Test
    fun isAcceptableTarget_movesDownToLowerPosition_isAccepted() {
        assertTrue(
            isAcceptableTarget(
                sourceIndex = 0,
                sourceTop = 0f,
                candidateTop = 400f,
                targetIndex = 4,
            ),
        )
    }

    @Test
    fun isAcceptableTarget_movesDownButShiftWithinEpsilon_isRejected() {
        assertFalse(
            isAcceptableTarget(
                sourceIndex = 0,
                sourceTop = 0f,
                candidateTop = TARGET_POSITION_EPSILON_PX,
                targetIndex = 4,
            ),
        )
    }

    @Test
    fun isAcceptableTarget_movesDownJustBeyondEpsilon_isAccepted() {
        assertTrue(
            isAcceptableTarget(
                sourceIndex = 0,
                sourceTop = 0f,
                candidateTop = TARGET_POSITION_EPSILON_PX + 0.5f,
                targetIndex = 4,
            ),
        )
    }

    @Test
    fun isAcceptableTarget_movesUpButShiftWithinEpsilon_isRejected() {
        assertFalse(
            isAcceptableTarget(
                sourceIndex = 3,
                sourceTop = 300f,
                candidateTop = 300f - TARGET_POSITION_EPSILON_PX,
                targetIndex = 0,
            ),
        )
    }

    @Test
    fun isAcceptableTarget_sameIndex_isAccepted() {
        assertTrue(
            isAcceptableTarget(
                sourceIndex = 2,
                sourceTop = 200f,
                candidateTop = 200f,
                targetIndex = 2,
            ),
        )
    }

    @Test
    fun fallbackTarget_anchorToTop_movesToContentTopWithinContainer() {
        val source = Rect(left = 10f, top = 500f, right = 110f, bottom = 560f)

        val target = geometry.fallbackTarget(
            sourceBounds = source,
            anchorToTop = true,
            contentTopInRoot = 80f,
            containerBounds = Rect(left = 0f, top = 30f, right = 200f, bottom = 1_000f),
        )

        assertEquals(50f, target.top)
        assertEquals(source.left, target.left)
        assertEquals(source.right, target.right)
        assertEquals(source.height, target.height)
    }

    @Test
    fun fallbackTarget_notAnchorToTop_movesBelowContainerBottom() {
        val source = Rect(left = 10f, top = 100f, right = 110f, bottom = 160f)
        val containerBounds = Rect(left = 0f, top = 0f, right = 200f, bottom = 1_000f)

        val target = geometry.fallbackTarget(
            sourceBounds = source,
            anchorToTop = false,
            contentTopInRoot = 0f,
            containerBounds = containerBounds,
        )

        assertTrue(target.top > containerBounds.height)
        assertEquals(source.height, target.height)
    }

    private fun isAcceptableTarget(
        isCommitted: Boolean = true,
        isStarted: Boolean = false,
        sourceIndex: Int = 3,
        sourceTop: Float = 300f,
        candidateTop: Float = 0f,
        targetIndex: Int = 0,
        isPhysicallyVisible: Boolean = true,
        isLogicallyVisible: Boolean = true,
        isModelSettled: Boolean = true,
    ): Boolean {
        return geometry.isAcceptableTarget(
            isCommitted = isCommitted,
            isStarted = isStarted,
            sourceIndex = sourceIndex,
            sourceTop = sourceTop,
            candidateTop = candidateTop,
            targetIndex = targetIndex,
            isPhysicallyVisible = isPhysicallyVisible,
            isLogicallyVisible = isLogicallyVisible,
            isModelSettled = isModelSettled,
        )
    }
}
