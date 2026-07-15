package com.android.messaging.ui.photoviewer.component

import androidx.compose.ui.unit.IntSize

private const val PHOTO_VIEWER_DECODE_SIZE_NUMERATOR = 3L
private const val PHOTO_VIEWER_DECODE_SIZE_DENOMINATOR = 2L
private const val PHOTO_VIEWER_MAX_DECODE_SIZE_PX = 4096L

internal fun resolvePhotoViewerDecodeSize(displayedImageSize: IntSize): IntSize {
    return IntSize(
        width = resolvePhotoViewerDecodeDimension(size = displayedImageSize.width),
        height = resolvePhotoViewerDecodeDimension(size = displayedImageSize.height),
    )
}

private fun resolvePhotoViewerDecodeDimension(size: Int): Int {
    return ceilScaledDecodeDimension(size = size.toLong())
        .coerceIn(
            minimumValue = 1L,
            maximumValue = PHOTO_VIEWER_MAX_DECODE_SIZE_PX,
        )
        .toInt()
}

private fun ceilScaledDecodeDimension(size: Long): Long {
    return (size * PHOTO_VIEWER_DECODE_SIZE_NUMERATOR + PHOTO_VIEWER_DECODE_SIZE_DENOMINATOR - 1L) /
        PHOTO_VIEWER_DECODE_SIZE_DENOMINATOR
}
