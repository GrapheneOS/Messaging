package com.android.messaging.ui.photoviewer.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs

internal fun Modifier.photoViewerTransformInput(
    contentKey: PhotoViewerContentKey,
    gestureState: ZoomablePhotoGestureState,
    currentOnEnterImmersiveMode: State<() -> Unit>,
): Modifier {
    return pointerInput(contentKey) {
        detectPhotoViewerTransformGestures(
            gestureState = gestureState,
            currentOnEnterImmersiveMode = currentOnEnterImmersiveMode,
        )
    }
}

private suspend fun PointerInputScope.detectPhotoViewerTransformGestures(
    gestureState: ZoomablePhotoGestureState,
    currentOnEnterImmersiveMode: State<() -> Unit>,
) {
    val touchSlop = viewConfiguration.touchSlop

    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var accumulatedZoom = 1f
        var accumulatedPan = Offset.Zero
        var isTransformActive = false
        var pressedPointerCount: Int

        do {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            pressedPointerCount = event.pressedPointerCount()
            val zoomChange = event.photoViewerZoomChange(pressedPointerCount = pressedPointerCount)
            val panChange = event.photoViewerPanChange(gestureState = gestureState)

            if (!isTransformActive) {
                accumulatedZoom *= zoomChange
                accumulatedPan += panChange
                isTransformActive = event.shouldStartPhotoViewerTransform(
                    gestureState = gestureState,
                    accumulatedZoom = accumulatedZoom,
                    accumulatedPan = accumulatedPan,
                    touchSlop = touchSlop,
                    pressedPointerCount = pressedPointerCount,
                )
            }

            if (isTransformActive) {
                handlePhotoViewerTransform(
                    event = event,
                    gestureState = gestureState,
                    zoomChange = zoomChange,
                    panChange = panChange,
                    currentOnEnterImmersiveMode = currentOnEnterImmersiveMode,
                )
            }
        } while (pressedPointerCount > 0)
    }
}

private fun PointerEvent.pressedPointerCount(): Int {
    return changes.count { change ->
        change.pressed
    }
}

private fun PointerEvent.photoViewerZoomChange(pressedPointerCount: Int): Float {
    return when {
        pressedPointerCount > 1 -> calculateZoom()
        else -> 1f
    }
}

private fun PointerEvent.photoViewerPanChange(
    gestureState: ZoomablePhotoGestureState,
): Offset {
    return when {
        gestureState.isZoomed() -> calculatePan()
        else -> Offset.Zero
    }
}

private fun PointerEvent.shouldStartPhotoViewerTransform(
    gestureState: ZoomablePhotoGestureState,
    accumulatedZoom: Float,
    accumulatedPan: Offset,
    touchSlop: Float,
    pressedPointerCount: Int,
): Boolean {
    val centroidSize = calculateCentroidSize(useCurrent = false)
    val zoomMotion = abs(1f - accumulatedZoom) * centroidSize
    val panMotion = accumulatedPan.getDistance()

    return when {
        pressedPointerCount > 1 && zoomMotion > touchSlop -> true
        gestureState.isZoomed() && panMotion > touchSlop -> true
        else -> false
    }
}

private fun handlePhotoViewerTransform(
    event: PointerEvent,
    gestureState: ZoomablePhotoGestureState,
    zoomChange: Float,
    panChange: Offset,
    currentOnEnterImmersiveMode: State<() -> Unit>,
) {
    if (gestureState.applyTransform(zoomChange = zoomChange, panChange = panChange)) {
        currentOnEnterImmersiveMode.value()
    }

    event.changes.forEach { change ->
        if (change.positionChanged()) {
            change.consume()
        }
    }
}
