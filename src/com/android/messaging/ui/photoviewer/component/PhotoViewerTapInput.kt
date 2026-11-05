package com.android.messaging.ui.photoviewer.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode

internal fun Modifier.photoViewerTapInput(
    contentKey: PhotoViewerContentKey,
    gestureState: ZoomablePhotoGestureState,
    currentDisplayMode: State<PhotoViewerDisplayMode>,
    currentOnToggleDisplayMode: State<() -> Unit>,
    currentOnEnterImmersiveMode: State<() -> Unit>,
): Modifier {
    return pointerInput(contentKey) {
        detectPhotoViewerTapGestures(
            isTapResolvedOnRelease = {
                currentDisplayMode.value == PhotoViewerDisplayMode.Carousel
            },
            onTap = {
                currentOnToggleDisplayMode.value()
            },
            onDoubleTap = { tapOffset ->
                currentOnEnterImmersiveMode.value()
                gestureState.applyDoubleTap(tapOffset = tapOffset)
            },
        )
    }
}

// Like detectTapGestures, but while isTapResolvedOnRelease holds, onTap runs on the first release
// instead of after the double-tap timeout; a following second tap still reports onDoubleTap.
internal suspend fun PointerInputScope.detectPhotoViewerTapGestures(
    isTapResolvedOnRelease: () -> Boolean,
    onTap: () -> Unit,
    onDoubleTap: (Offset) -> Unit,
) {
    awaitEachGesture {
        val firstUp = awaitPhotoViewerTapUp()

        if (firstUp != null) {
            val isTapResolved = isTapResolvedOnRelease()
            if (isTapResolved) {
                onTap()
            }

            val secondUp = awaitPhotoViewerSecondTapUp(firstUp = firstUp)
            when {
                secondUp != null -> onDoubleTap(secondUp.position)
                !isTapResolved -> onTap()
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitPhotoViewerTapUp(): PointerInputChange? {
    awaitFirstDown().consume()

    val up = waitForUpOrCancellation()
    up?.consume()

    return up
}

private suspend fun AwaitPointerEventScope.awaitPhotoViewerSecondTapUp(
    firstUp: PointerInputChange,
): PointerInputChange? {
    val secondDown = withTimeoutOrNull(timeMillis = viewConfiguration.doubleTapTimeoutMillis) {
        val minimumUptimeMillis = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
        var down: PointerInputChange
        do {
            down = awaitFirstDown()
        } while (down.uptimeMillis < minimumUptimeMillis)
        down
    }

    val secondUp = secondDown?.let {
        waitForUpOrCancellation()
    }
    secondUp?.consume()

    return secondUp
}
