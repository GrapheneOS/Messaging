package com.android.messaging.ui.photoviewer.component

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

private const val PHOTO_VIEWER_DOUBLE_TAP_SCALE = 2.5f
private const val PHOTO_VIEWER_MAX_SCALE = 5f
private const val PHOTO_VIEWER_ZOOM_EPSILON = 0.01f

@Stable
internal class ZoomablePhotoGestureState {
    var containerSize by mutableStateOf(value = IntSize.Zero)
        private set

    var scale by mutableFloatStateOf(value = 1f)
        private set

    var offset by mutableStateOf(value = Offset.Zero)
        private set

    var isZoomAnimationEnabled by mutableStateOf(value = false)
        private set

    fun updateContainerSize(containerSize: IntSize) {
        this.containerSize = containerSize
        offset = coerceOffset(
            candidateOffset = offset,
            candidateScale = scale,
        )
    }

    fun isZoomed(): Boolean {
        return scale > 1f + PHOTO_VIEWER_ZOOM_EPSILON
    }

    fun applyTransform(zoomChange: Float, panChange: Offset): Boolean {
        isZoomAnimationEnabled = false
        val nextScale = (scale * zoomChange).coerceIn(
            minimumValue = 1f,
            maximumValue = PHOTO_VIEWER_MAX_SCALE,
        )

        scale = nextScale
        offset = coerceOffset(
            candidateOffset = offset + panChange,
            candidateScale = nextScale,
        )

        return zoomChange != 1f
    }

    fun applyDoubleTap(tapOffset: Offset) {
        isZoomAnimationEnabled = true
        val nextScale = when {
            isZoomed() -> 1f
            else -> PHOTO_VIEWER_DOUBLE_TAP_SCALE
        }

        scale = nextScale
        offset = doubleTapOffset(
            tapOffset = tapOffset,
            nextScale = nextScale,
        )
    }

    fun resetAll() {
        isZoomAnimationEnabled = false
        scale = 1f
        offset = Offset.Zero
    }

    private fun coerceOffset(candidateOffset: Offset, candidateScale: Float): Offset {
        if (candidateScale <= 1f || containerSize == IntSize.Zero) {
            return Offset.Zero
        }

        val maxX = containerSize.width * (candidateScale - 1f) / 2f
        val maxY = containerSize.height * (candidateScale - 1f) / 2f

        return Offset(
            x = candidateOffset.x.coerceIn(
                minimumValue = -maxX,
                maximumValue = maxX,
            ),
            y = candidateOffset.y.coerceIn(
                minimumValue = -maxY,
                maximumValue = maxY,
            ),
        )
    }

    private fun doubleTapOffset(tapOffset: Offset, nextScale: Float): Offset {
        val containerCenter = Offset(
            x = containerSize.width / 2f,
            y = containerSize.height / 2f,
        )
        val tapOffsetFromCenter = tapOffset - containerCenter

        return when {
            nextScale <= 1f -> Offset.Zero
            containerSize == IntSize.Zero -> Offset.Zero
            else -> {
                coerceOffset(
                    candidateOffset = -tapOffsetFromCenter * (nextScale - 1f),
                    candidateScale = nextScale,
                )
            }
        }
    }
}
