package com.android.messaging.ui.photoviewer.component

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode
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
    val transformableState = rememberTransformableState { _, zoomChange, panChange, _ ->
        if (gestureState.applyTransform(zoomChange = zoomChange, panChange = panChange)) {
            currentOnEnterImmersiveMode.value()
        }
    }
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
                transformableState = transformableState,
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
    transformableState: TransformableState,
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
        .photoViewerDismissDragInput(
            contentKey = contentKey,
            gestureState = gestureState,
            dismissDragState = dismissDragState,
            currentOnCloseClick = currentOnCloseClick,
        )
        .transformable(
            state = transformableState,
            canPan = {
                gestureState.isZoomed()
            },
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
    return pointerInput(
        contentKey,
        dismissDragState,
    ) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                dismissDragState?.let { activeDismissDragState ->
                    val canHandleDismissDrag = activeDismissDragState.canHandleDrag(
                        isZoomed = gestureState.isZoomed(),
                        dragAmount = dragAmount,
                    )

                    if (canHandleDismissDrag) {
                        change.consume()
                        activeDismissDragState.activate()
                        activeDismissDragState.applyDrag(dragAmount = dragAmount)
                    }
                }
            },
            onDragEnd = {
                when {
                    dismissDragState?.shouldCloseOnDragEnd() == true -> {
                        currentOnCloseClick.value()
                    }

                    else -> {
                        dismissDragState?.reset()
                    }
                }
            },
            onDragCancel = {
                dismissDragState?.reset()
            },
        )
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
