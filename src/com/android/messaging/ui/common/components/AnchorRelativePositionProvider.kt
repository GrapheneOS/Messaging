package com.android.messaging.ui.common.components

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

internal class AnchorRelativePositionProvider(
    private val anchorBoundsPx: IntRect,
    private val gapPx: Int,
    private val transformOriginState: MutableState<TransformOrigin>,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val popupWidth = popupContentSize.width
        val popupHeight = popupContentSize.height

        val rightX = anchorBoundsPx.left
        val leftX = anchorBoundsPx.right - popupWidth
        val fitsRight = rightX + popupWidth <= windowSize.width
        val (x, originX) = when {
            fitsRight -> rightX to 0f
            leftX >= 0 -> leftX to 1f
            else -> rightX.coerceAtMost(windowSize.width - popupWidth).coerceAtLeast(0) to 0f
        }

        val aboveY = anchorBoundsPx.top - gapPx - popupHeight
        val belowY = anchorBoundsPx.bottom + gapPx
        val fitsAbove = aboveY >= 0
        val (y, originY) = when {
            fitsAbove -> aboveY to 1f
            belowY + popupHeight <= windowSize.height -> belowY to 0f
            else -> aboveY.coerceAtLeast(0) to 1f
        }

        transformOriginState.value = TransformOrigin(originX, originY)

        return IntOffset(x, y)
    }
}
