package com.android.messaging.ui.common.components.reorder

import androidx.compose.ui.geometry.Rect

internal const val TARGET_POSITION_EPSILON_PX = 1f

private const val OFFSCREEN_TARGET_GAP_RATIO = 0.5f

internal class OverlayReorderGeometry {

    fun isAcceptableTarget(
        isCommitted: Boolean,
        isStarted: Boolean,
        sourceIndex: Int,
        sourceTop: Float,
        candidateTop: Float,
        targetIndex: Int,
        isPhysicallyVisible: Boolean,
        isLogicallyVisible: Boolean,
        isModelSettled: Boolean,
    ): Boolean {
        val isRejected = when {
            !isCommitted -> true
            isStarted -> true
            !isPhysicallyVisible -> true
            !isLogicallyVisible -> true
            !isModelSettled -> true
            else -> false
        }

        if (isRejected) {
            return false
        }

        return hasExpectedDirection(
            sourceIndex = sourceIndex,
            targetIndex = targetIndex,
            sourceTop = sourceTop,
            candidateTop = candidateTop,
        )
    }

    fun fallbackTarget(
        sourceBounds: Rect,
        anchorToTop: Boolean,
        contentTopInRoot: Float,
        containerBounds: Rect,
    ): Rect {
        val targetTop = when {
            anchorToTop -> contentTopInRoot - containerBounds.top
            else -> containerBounds.height + sourceBounds.height * OFFSCREEN_TARGET_GAP_RATIO
        }

        return Rect(
            left = sourceBounds.left,
            top = targetTop,
            right = sourceBounds.right,
            bottom = targetTop + sourceBounds.height,
        )
    }

    private fun hasExpectedDirection(
        sourceIndex: Int,
        targetIndex: Int,
        sourceTop: Float,
        candidateTop: Float,
    ): Boolean {
        val verticalShift = candidateTop - sourceTop

        return when {
            targetIndex > sourceIndex -> verticalShift > TARGET_POSITION_EPSILON_PX
            targetIndex < sourceIndex -> verticalShift < -TARGET_POSITION_EPSILON_PX
            else -> true
        }
    }
}
