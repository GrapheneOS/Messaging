package com.android.messaging.ui.common.components

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

internal class AnchorRelativePositionProvider(
    private val gapPx: Int,
    private val contentPaddingPx: Int,
    private val transformOriginState: MutableState<TransformOrigin>,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val cardWidth = popupContentSize.width - 2 * contentPaddingPx
        val cardHeight = popupContentSize.height - 2 * contentPaddingPx

        val horizontalRange = 0..(windowSize.width - cardWidth).coerceAtLeast(0)
        val verticalRange = 0..(windowSize.height - cardHeight).coerceAtLeast(0)

        val leftAlignedX = anchorBounds.left
        val rightAlignedX = anchorBounds.right - cardWidth
        val alignLeft = leftAlignedX in horizontalRange
        val cardX = when {
            alignLeft -> leftAlignedX
            rightAlignedX in horizontalRange -> rightAlignedX
            else -> leftAlignedX.coerceIn(horizontalRange)
        }

        val aboveY = anchorBounds.top - gapPx - cardHeight
        val belowY = anchorBounds.bottom + gapPx
        val placeAbove = aboveY in verticalRange
        val cardY = when {
            placeAbove -> aboveY
            belowY in verticalRange -> belowY
            else -> aboveY.coerceIn(verticalRange)
        }

        val pivotX = when {
            alignLeft -> contentPaddingPx
            else -> contentPaddingPx + cardWidth
        }
        val pivotY = when {
            placeAbove -> contentPaddingPx + cardHeight
            else -> contentPaddingPx
        }

        transformOriginState.value = TransformOrigin(
            pivotFractionX = pivotX.toFloat() / popupContentSize.width,
            pivotFractionY = pivotY.toFloat() / popupContentSize.height,
        )

        return IntOffset(
            x = cardX - contentPaddingPx,
            y = cardY - contentPaddingPx,
        )
    }
}
