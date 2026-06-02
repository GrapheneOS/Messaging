package com.android.messaging.ui.common.components.attachment

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.net.Uri
import android.util.Size
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.scale
import com.android.messaging.util.ContentType
import com.android.messaging.util.MediaMetadataRetrieverWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val FIRST_PASS_DIVISOR = 12
private const val FIRST_PASS_MAXIMUM_SIZE = 24
private const val FIRST_PASS_MINIMUM_SIZE = 12
private const val SECOND_PASS_DIVISOR = 3
private const val SECOND_PASS_MAXIMUM_SIZE = 48
private const val SECOND_PASS_MINIMUM_SIZE = 24

internal suspend fun loadMediaThumbnailBitmap(
    contentResolver: ContentResolver,
    contentUri: Uri,
    contentType: String,
    size: IntSize,
    softenBitmap: Boolean,
): Bitmap? {
    return withContext(context = Dispatchers.IO) {
        val rawBitmap = loadPlatformThumbnail(
            contentResolver = contentResolver,
            contentUri = contentUri,
            size = size,
        ) ?: loadFallbackBitmap(
            contentResolver = contentResolver,
            contentUri = contentUri,
            contentType = contentType,
            size = size,
        )

        maybeSoftenBitmap(
            bitmap = rawBitmap,
            outputSize = size,
            softenBitmap = softenBitmap,
        )
    }
}

private fun loadPlatformThumbnail(
    contentResolver: ContentResolver,
    contentUri: Uri,
    size: IntSize,
): Bitmap? {
    return runCatching {
        contentResolver.loadThumbnail(
            contentUri,
            Size(size.width, size.height),
            null,
        )
    }.getOrNull()
}

private fun loadFallbackBitmap(
    contentResolver: ContentResolver,
    contentUri: Uri,
    contentType: String,
    size: IntSize,
): Bitmap? {
    return when {
        ContentType.isImageType(contentType) -> {
            loadImageBitmapFallback(
                contentResolver = contentResolver,
                contentUri = contentUri,
                size = size,
            )
        }

        ContentType.isVideoType(contentType) -> {
            loadVideoFrameFallback(
                contentUri = contentUri,
                size = size,
            )
        }

        else -> null
    }
}

private fun loadImageBitmapFallback(
    contentResolver: ContentResolver,
    contentUri: Uri,
    size: IntSize,
): Bitmap? {
    return runCatching {
        val decodeBoundsOptions = Options().apply {
            inJustDecodeBounds = true
        }

        contentResolver
            .openInputStream(contentUri)
            ?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeBoundsOptions)
            }

        val decodeBitmapOptions = Options().apply {
            inSampleSize = calculateBitmapSampleSize(
                sourceWidth = decodeBoundsOptions.outWidth,
                sourceHeight = decodeBoundsOptions.outHeight,
                targetSize = size,
            )
        }

        contentResolver
            .openInputStream(contentUri)
            ?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeBitmapOptions)
            }
    }.getOrNull()
}

private fun loadVideoFrameFallback(
    contentUri: Uri,
    size: IntSize,
): Bitmap? {
    val retriever = MediaMetadataRetrieverWrapper()

    return try {
        runCatching {
            retriever.setDataSource(contentUri)
            retriever.frameAtTime?.let { bitmap ->
                scaleBitmapDownIfNeeded(
                    bitmap = bitmap,
                    targetSize = size,
                )
            }
        }.getOrNull()
    } finally {
        retriever.release()
    }
}

private fun maybeSoftenBitmap(
    bitmap: Bitmap?,
    outputSize: IntSize,
    softenBitmap: Boolean,
): Bitmap? {
    return when {
        bitmap == null -> null
        !softenBitmap -> bitmap

        else -> {
            createSoftenedBitmap(
                sourceBitmap = bitmap,
                outputSize = outputSize,
            )
        }
    }
}

private fun createSoftenedBitmap(
    sourceBitmap: Bitmap,
    outputSize: IntSize,
): Bitmap {
    val sanitizedOutputSize = outputSize.sanitized()
    val targetWidth = sanitizedOutputSize.width
    val targetHeight = sanitizedOutputSize.height
    val centerCroppedBitmap = createCenterCroppedBitmap(
        sourceBitmap = sourceBitmap,
        targetSize = sanitizedOutputSize,
    )

    // Multi-pass downscaling keeps softened placeholders smooth without introducing blur kernels
    val firstPassWidth = (targetWidth / FIRST_PASS_DIVISOR).coerceIn(
        minimumValue = FIRST_PASS_MINIMUM_SIZE,
        maximumValue = FIRST_PASS_MAXIMUM_SIZE,
    )
    val firstPassHeight = (targetHeight / FIRST_PASS_DIVISOR).coerceIn(
        minimumValue = FIRST_PASS_MINIMUM_SIZE,
        maximumValue = FIRST_PASS_MAXIMUM_SIZE,
    )
    val secondPassWidth = (targetWidth / SECOND_PASS_DIVISOR).coerceIn(
        minimumValue = SECOND_PASS_MINIMUM_SIZE,
        maximumValue = SECOND_PASS_MAXIMUM_SIZE,
    )
    val secondPassHeight = (targetHeight / SECOND_PASS_DIVISOR).coerceIn(
        minimumValue = SECOND_PASS_MINIMUM_SIZE,
        maximumValue = SECOND_PASS_MAXIMUM_SIZE,
    )

    val firstPassBitmap = centerCroppedBitmap.scale(firstPassWidth, firstPassHeight)
    val secondPassBitmap = firstPassBitmap.scale(secondPassWidth, secondPassHeight)

    return secondPassBitmap.scale(targetWidth, targetHeight)
}

private fun createCenterCroppedBitmap(
    sourceBitmap: Bitmap,
    targetSize: IntSize,
): Bitmap {
    val sanitizedTargetSize = targetSize.sanitized()
    val targetAspectRatio =
        sanitizedTargetSize.width.toFloat() / sanitizedTargetSize.height.toFloat()
    val sourceAspectRatio = sourceBitmap.width.toFloat() / sourceBitmap.height.toFloat()

    val cropWidth: Int
    val cropHeight: Int

    when {
        sourceAspectRatio > targetAspectRatio -> {
            cropHeight = sourceBitmap.height
            cropWidth = (cropHeight * targetAspectRatio).toInt()
        }

        else -> {
            cropWidth = sourceBitmap.width
            cropHeight = (cropWidth / targetAspectRatio).toInt()
        }
    }

    val left = (sourceBitmap.width - cropWidth) / 2
    val top = (sourceBitmap.height - cropHeight) / 2

    return Bitmap.createBitmap(
        sourceBitmap,
        left,
        top,
        cropWidth.coerceAtLeast(minimumValue = 1),
        cropHeight.coerceAtLeast(minimumValue = 1),
    )
}

private fun calculateBitmapSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    targetSize: IntSize,
): Int {
    if (sourceWidth <= 0 || sourceHeight <= 0) {
        return 1
    }

    var sampleSize = 1
    val targetWidth = targetSize.width.coerceAtLeast(minimumValue = 1)
    val targetHeight = targetSize.height.coerceAtLeast(minimumValue = 1)

    while (
        canDoubleBitmapSampleSize(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            sampleSize = sampleSize,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
        )
    ) {
        sampleSize *= 2
    }

    return sampleSize
}

private fun canDoubleBitmapSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    sampleSize: Int,
    targetWidth: Int,
    targetHeight: Int,
): Boolean {
    val doubledSampleSize = sampleSize * 2
    val doubledDecodedWidth = sourceWidth / doubledSampleSize
    val doubledDecodedHeight = sourceHeight / doubledSampleSize

    return doubledDecodedWidth >= targetWidth &&
        doubledDecodedHeight >= targetHeight
}

private fun scaleBitmapDownIfNeeded(
    bitmap: Bitmap,
    targetSize: IntSize,
): Bitmap {
    val targetWidth = targetSize.width.coerceAtLeast(minimumValue = 1)
    val targetHeight = targetSize.height.coerceAtLeast(minimumValue = 1)

    if (bitmap.width <= targetWidth && bitmap.height <= targetHeight) {
        return bitmap
    }

    val widthScale = targetWidth.toFloat() / bitmap.width.toFloat()
    val heightScale = targetHeight.toFloat() / bitmap.height.toFloat()
    val scale = minOf(widthScale, heightScale)

    return bitmap.scale(
        width = (bitmap.width * scale).toInt().coerceAtLeast(minimumValue = 1),
        height = (bitmap.height * scale).toInt().coerceAtLeast(minimumValue = 1),
    )
}

internal fun IntSize.sanitized(): IntSize {
    if (width >= 1 && height >= 1) {
        return this
    }

    return IntSize(
        width = width.coerceAtLeast(minimumValue = 1),
        height = height.coerceAtLeast(minimumValue = 1),
    )
}
