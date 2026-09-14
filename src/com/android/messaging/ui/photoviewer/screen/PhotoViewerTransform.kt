package com.android.messaging.ui.photoviewer.screen

import androidx.compose.ui.unit.IntSize
import com.android.messaging.ui.photoviewer.model.PhotoViewerSourceBounds

private const val PHOTO_VIEWER_FALLBACK_ENTRY_SCALE = 0.95f
private const val PHOTO_VIEWER_FALLBACK_ENTRY_TRANSLATION_Y = 24f
private const val PHOTO_VIEWER_MIN_SOURCE_BOUNDS_SCALE = 0.1f

internal fun resolvePhotoViewerTransform(
    sourceBounds: PhotoViewerSourceBounds,
    rootSize: IntSize,
    progress: Float,
): PhotoViewerTransform {
    if (rootSize == IntSize.Zero) {
        return PhotoViewerTransform(
            scale = PHOTO_VIEWER_FALLBACK_ENTRY_SCALE,
            alpha = 0f,
        )
    }

    val hasValidSourceBounds = sourceBounds.width > 0 && sourceBounds.height > 0

    val initialScale = when {
        hasValidSourceBounds -> {
            maxOf(
                sourceBounds.width.toFloat() / rootSize.width.toFloat(),
                sourceBounds.height.toFloat() / rootSize.height.toFloat(),
            ).coerceIn(
                minimumValue = PHOTO_VIEWER_MIN_SOURCE_BOUNDS_SCALE,
                maximumValue = 1f,
            )
        }

        else -> PHOTO_VIEWER_FALLBACK_ENTRY_SCALE
    }

    return PhotoViewerTransform(
        scale = interpolatePhotoViewerTransformValue(
            start = initialScale,
            stop = 1f,
            fraction = progress,
        ),
        translationX = initialPhotoViewerTranslationX(
            sourceBounds = sourceBounds,
            rootSize = rootSize,
            hasValidSourceBounds = hasValidSourceBounds,
        ).let { translation ->
            interpolatePhotoViewerTransformValue(
                start = translation,
                stop = 0f,
                fraction = progress,
            )
        },
        translationY = initialPhotoViewerTranslationY(
            sourceBounds = sourceBounds,
            rootSize = rootSize,
            hasValidSourceBounds = hasValidSourceBounds,
        ).let { translation ->
            interpolatePhotoViewerTransformValue(
                start = translation,
                stop = 0f,
                fraction = progress,
            )
        },
        alpha = interpolatePhotoViewerTransformValue(
            start = 0f,
            stop = 1f,
            fraction = progress,
        ),
    )
}

private fun initialPhotoViewerTranslationX(
    sourceBounds: PhotoViewerSourceBounds,
    rootSize: IntSize,
    hasValidSourceBounds: Boolean,
): Float {
    val rootCenterX = rootSize.width / 2f

    return when {
        hasValidSourceBounds -> sourceBounds.centerX - rootCenterX
        else -> 0f
    }
}

private fun initialPhotoViewerTranslationY(
    sourceBounds: PhotoViewerSourceBounds,
    rootSize: IntSize,
    hasValidSourceBounds: Boolean,
): Float {
    val rootCenterY = rootSize.height / 2f

    return when {
        hasValidSourceBounds -> sourceBounds.centerY - rootCenterY
        else -> PHOTO_VIEWER_FALLBACK_ENTRY_TRANSLATION_Y
    }
}

private fun interpolatePhotoViewerTransformValue(
    start: Float,
    stop: Float,
    fraction: Float,
): Float {
    return start + (stop - start) * fraction
}

internal data class PhotoViewerTransform(
    val scale: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val alpha: Float = 1f,
)
