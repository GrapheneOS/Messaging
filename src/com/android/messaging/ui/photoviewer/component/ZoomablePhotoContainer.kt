package com.android.messaging.ui.photoviewer.component

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

private const val PHOTO_VIEWER_DOUBLE_TAP_ZOOM_ANIMATION_DURATION_MILLIS = 200

@Composable
internal fun ZoomablePhotoContainer(
    modifier: Modifier,
    contentKey: PhotoViewerContentKey,
    displayMode: PhotoViewerDisplayMode,
    onToggleDisplayMode: () -> Unit,
    onEnterImmersiveMode: () -> Unit,
    onCloseClick: () -> Unit,
    dismissDragState: PhotoViewerDismissDragState?,
    onZoomChanged: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val gestureState = remember(contentKey) { ZoomablePhotoGestureState() }
    val currentOnToggleDisplayMode = rememberUpdatedState(newValue = onToggleDisplayMode)
    val currentOnEnterImmersiveMode = rememberUpdatedState(newValue = onEnterImmersiveMode)
    val currentOnCloseClick = rememberUpdatedState(newValue = onCloseClick)
    val zoomAnimationSpec = photoViewerZoomAnimationSpec<Float>(
        isEnabled = gestureState.isZoomAnimationEnabled,
    )
    val offsetAnimationSpec = photoViewerZoomAnimationSpec<Offset>(
        isEnabled = gestureState.isZoomAnimationEnabled,
    )
    val animatedScale = animateFloatAsState(
        targetValue = gestureState.scale,
        animationSpec = zoomAnimationSpec,
        label = "PhotoViewerZoomScale",
    )
    val animatedOffset = animateOffsetAsState(
        targetValue = gestureState.offset,
        animationSpec = offsetAnimationSpec,
        label = "PhotoViewerZoomOffset",
    )

    PhotoViewerGestureEffects(
        contentKey = contentKey,
        displayMode = displayMode,
        gestureState = gestureState,
        dismissDragState = dismissDragState,
        onZoomChanged = onZoomChanged,
    )

    Box(
        modifier = modifier
            .photoViewerGestureModifier(
                contentKey = contentKey,
                gestureState = gestureState,
                animatedScale = animatedScale,
                animatedOffset = animatedOffset,
                dismissDragState = dismissDragState,
                currentOnToggleDisplayMode = currentOnToggleDisplayMode,
                currentOnEnterImmersiveMode = currentOnEnterImmersiveMode,
                currentOnCloseClick = currentOnCloseClick,
            ),
    ) {
        content()
    }
}

@Composable
private fun PhotoViewerGestureEffects(
    contentKey: PhotoViewerContentKey,
    displayMode: PhotoViewerDisplayMode,
    gestureState: ZoomablePhotoGestureState,
    dismissDragState: PhotoViewerDismissDragState?,
    onZoomChanged: (Boolean) -> Unit,
) {
    val currentOnZoomChanged by rememberUpdatedState(newValue = onZoomChanged)

    LaunchedEffect(gestureState) {
        snapshotFlow { gestureState.isZoomed() }
            .distinctUntilChanged()
            .collect { isZoomed ->
                currentOnZoomChanged(isZoomed)
            }
    }

    LaunchedEffect(displayMode, contentKey) {
        if (displayMode == PhotoViewerDisplayMode.Carousel) {
            gestureState.resetAll()
            dismissDragState?.reset()
        }
    }
}

private fun Modifier.photoViewerGestureModifier(
    contentKey: PhotoViewerContentKey,
    gestureState: ZoomablePhotoGestureState,
    animatedScale: State<Float>,
    animatedOffset: State<Offset>,
    dismissDragState: PhotoViewerDismissDragState?,
    currentOnToggleDisplayMode: State<() -> Unit>,
    currentOnEnterImmersiveMode: State<() -> Unit>,
    currentOnCloseClick: State<() -> Unit>,
): Modifier {
    return onSizeChanged { size ->
        gestureState.updateContainerSize(containerSize = size)
    }
        .photoViewerTapInput(
            contentKey = contentKey,
            gestureState = gestureState,
            currentOnToggleDisplayMode = currentOnToggleDisplayMode,
            currentOnEnterImmersiveMode = currentOnEnterImmersiveMode,
        )
        .photoViewerTransformInput(
            contentKey = contentKey,
            gestureState = gestureState,
            currentOnEnterImmersiveMode = currentOnEnterImmersiveMode,
        )
        .photoViewerDismissDragInput(
            contentKey = contentKey,
            gestureState = gestureState,
            dismissDragState = dismissDragState,
            currentOnCloseClick = currentOnCloseClick,
        )
        .graphicsLayer {
            val dismissDragOffset = dismissDragState?.animatedDragOffset ?: 0f
            val zoomScale = animatedScale.value
            val zoomOffset = animatedOffset.value

            alpha = dismissDragState?.imageAlpha ?: 1f
            scaleX = zoomScale
            scaleY = zoomScale
            translationX = zoomOffset.x
            translationY = zoomOffset.y + dismissDragOffset
        }
}

private fun Modifier.photoViewerTapInput(
    contentKey: PhotoViewerContentKey,
    gestureState: ZoomablePhotoGestureState,
    currentOnToggleDisplayMode: State<() -> Unit>,
    currentOnEnterImmersiveMode: State<() -> Unit>,
): Modifier {
    return pointerInput(contentKey) {
        detectTapGestures(
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

private fun Modifier.photoViewerDismissDragInput(
    contentKey: PhotoViewerContentKey,
    gestureState: ZoomablePhotoGestureState,
    dismissDragState: PhotoViewerDismissDragState?,
    currentOnCloseClick: State<() -> Unit>,
): Modifier {
    return when {
        dismissDragState == null -> this
        else -> {
            pointerInput(
                contentKey,
                dismissDragState,
            ) {
                detectPhotoViewerDismissDrag(
                    gestureState = gestureState,
                    dismissDragState = dismissDragState,
                    currentOnCloseClick = currentOnCloseClick,
                )
            }
        }
    }
}

private suspend fun PointerInputScope.detectPhotoViewerDismissDrag(
    gestureState: ZoomablePhotoGestureState,
    dismissDragState: PhotoViewerDismissDragState,
    currentOnCloseClick: State<() -> Unit>,
) {
    val touchSlop = viewConfiguration.touchSlop

    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var accumulatedDrag = Offset.Zero
        var isDismissDragActive = false
        var isGestureActive = true

        while (isGestureActive) {
            val event = awaitPointerEvent()
            val pressedChanges = event.changes.filter { change ->
                change.pressed
            }

            when {
                pressedChanges.isEmpty() -> {
                    finishPhotoViewerDismissDrag(
                        dismissDragState = dismissDragState,
                        currentOnCloseClick = currentOnCloseClick,
                    )
                    isGestureActive = false
                }

                pressedChanges.size > 1 -> {
                    dismissDragState.reset()
                    isGestureActive = false
                }

                else -> {
                    val change = pressedChanges.first()
                    val positionChange = change.positionChange()
                    accumulatedDrag += positionChange

                    isDismissDragActive = handlePhotoViewerDismissDrag(
                        gestureState = gestureState,
                        dismissDragState = dismissDragState,
                        accumulatedDrag = accumulatedDrag,
                        dragAmount = positionChange.y,
                        isDismissDragActive = isDismissDragActive,
                        touchSlop = touchSlop,
                    )

                    if (isDismissDragActive) {
                        change.consume()
                    }
                }
            }
        }
    }
}

private fun handlePhotoViewerDismissDrag(
    gestureState: ZoomablePhotoGestureState,
    dismissDragState: PhotoViewerDismissDragState,
    accumulatedDrag: Offset,
    dragAmount: Float,
    isDismissDragActive: Boolean,
    touchSlop: Float,
): Boolean {
    val canHandleDismissDrag = dismissDragState.canHandleDrag(
        isZoomed = gestureState.isZoomed(),
        dragAmount = dragAmount,
    )
    val shouldStartDismissDrag = isDismissDragActive ||
        accumulatedDrag.shouldStartPhotoViewerDismissDrag(touchSlop = touchSlop)

    val dismissDragAmount = when {
        isDismissDragActive -> dragAmount
        else -> {
            (accumulatedDrag.y - touchSlop)
                .coerceAtLeast(minimumValue = 0f)
        }
    }

    if (canHandleDismissDrag && shouldStartDismissDrag) {
        dismissDragState.activate()
        dismissDragState.applyDrag(dragAmount = dismissDragAmount)
    }

    return canHandleDismissDrag && shouldStartDismissDrag
}

private fun Offset.shouldStartPhotoViewerDismissDrag(touchSlop: Float): Boolean {
    return y > touchSlop && abs(y) >= abs(x)
}

private fun finishPhotoViewerDismissDrag(
    dismissDragState: PhotoViewerDismissDragState,
    currentOnCloseClick: State<() -> Unit>,
) {
    when {
        dismissDragState.shouldCloseOnDragEnd() -> {
            currentOnCloseClick.value()
        }

        else -> {
            dismissDragState.reset()
        }
    }
}

private fun <T> photoViewerZoomAnimationSpec(isEnabled: Boolean): AnimationSpec<T> {
    return when {
        isEnabled -> tween(
            durationMillis = PHOTO_VIEWER_DOUBLE_TAP_ZOOM_ANIMATION_DURATION_MILLIS,
            easing = FastOutSlowInEasing,
        )

        else -> snap()
    }
}
