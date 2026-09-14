package com.android.messaging.ui.photoviewer.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequestKey

private const val PHOTO_VIEWER_BACKGROUND_DISMISS_TRANSPARENT_PROGRESS = 0.5f
private const val PHOTO_VIEWER_TOP_BAR_DISMISS_HIDE_PROGRESS = 0.1f
private const val PHOTO_VIEWER_DISMISS_CLOSE_PROGRESS = 0.5f
private const val PHOTO_VIEWER_DISMISS_RETURN_ANIMATION_MILLIS = 200
private const val PHOTO_VIEWER_IMAGE_DISMISS_MIN_ALPHA = 0.4f

private val PhotoViewerDismissDragThreshold = 180.dp

@Composable
internal fun rememberPhotoViewerDismissDragState(
    resetKey: PhotoViewerLaunchRequestKey,
): PhotoViewerDismissDragState {
    val swipeDismissThreshold = with(LocalDensity.current) {
        PhotoViewerDismissDragThreshold.toPx()
    }
    val dismissDragOffsetState = remember(resetKey, swipeDismissThreshold) {
        mutableFloatStateOf(value = 0f)
    }
    val isActiveState = remember(resetKey, swipeDismissThreshold) {
        mutableStateOf(value = false)
    }
    val animatedDismissDragOffsetState = animateFloatAsState(
        targetValue = dismissDragOffsetState.floatValue,
        animationSpec = when {
            isActiveState.value -> snap()
            else -> tween(durationMillis = PHOTO_VIEWER_DISMISS_RETURN_ANIMATION_MILLIS)
        },
        label = "photoViewerDismissDragOffset",
    )

    return remember(
        swipeDismissThreshold,
        dismissDragOffsetState,
        isActiveState,
        animatedDismissDragOffsetState,
    ) {
        PhotoViewerDismissDragState(
            swipeDismissThreshold = swipeDismissThreshold,
            dismissDragOffsetState = dismissDragOffsetState,
            isActiveState = isActiveState,
            animatedDismissDragOffsetState = animatedDismissDragOffsetState,
        )
    }
}

@Stable
internal class PhotoViewerDismissDragState(
    private val swipeDismissThreshold: Float,
    private val dismissDragOffsetState: MutableFloatState,
    private val isActiveState: MutableState<Boolean>,
    private val animatedDismissDragOffsetState: State<Float>,
) {
    val animatedDragOffset: Float
        get() = animatedDismissDragOffsetState.value

    val animatedProgress: Float
        get() = dismissDragProgress(dismissDragOffset = animatedDragOffset)

    val backgroundAlpha: Float
        get() = (1f - animatedProgress / PHOTO_VIEWER_BACKGROUND_DISMISS_TRANSPARENT_PROGRESS)
            .coerceIn(
                minimumValue = 0f,
                maximumValue = 1f,
            )

    val imageAlpha: Float
        get() = (1f - animatedProgress)
            .coerceIn(
                minimumValue = PHOTO_VIEWER_IMAGE_DISMISS_MIN_ALPHA,
                maximumValue = 1f,
            )

    val shouldShowTopBar: Boolean
        get() = animatedProgress < PHOTO_VIEWER_TOP_BAR_DISMISS_HIDE_PROGRESS

    fun canHandleDrag(
        isZoomed: Boolean,
        dragAmount: Float,
    ): Boolean {
        return !isZoomed && (dragAmount > 0f || dismissDragOffsetState.floatValue > 0f)
    }

    fun activate() {
        isActiveState.value = true
    }

    fun applyDrag(dragAmount: Float) {
        dismissDragOffsetState.floatValue = (dismissDragOffsetState.floatValue + dragAmount)
            .coerceAtLeast(minimumValue = 0f)
    }

    fun shouldCloseOnDragEnd(): Boolean {
        return dismissDragOffsetState.floatValue >=
            swipeDismissThreshold * PHOTO_VIEWER_DISMISS_CLOSE_PROGRESS
    }

    fun reset() {
        dismissDragOffsetState.floatValue = 0f
        isActiveState.value = false
    }

    private fun dismissDragProgress(dismissDragOffset: Float): Float {
        return when {
            swipeDismissThreshold <= 0f -> 0f

            else -> {
                (dismissDragOffset / swipeDismissThreshold).coerceIn(
                    minimumValue = 0f,
                    maximumValue = 1f,
                )
            }
        }
    }
}
