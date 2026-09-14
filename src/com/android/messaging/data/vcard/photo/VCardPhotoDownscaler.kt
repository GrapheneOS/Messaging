package com.android.messaging.data.vcard.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.android.messaging.util.LogUtil
import java.io.ByteArrayOutputStream
import javax.inject.Inject

internal interface VCardPhotoDownscaler {
    fun downscale(photoBytes: ByteArray): ByteArray?
}

internal class VCardPhotoDownscalerImpl @Inject constructor() : VCardPhotoDownscaler {

    override fun downscale(photoBytes: ByteArray): ByteArray? {
        if (photoBytes.isEmpty()) {
            return null
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size, bounds)

        val hasValidBounds = bounds.outWidth > 0 && bounds.outHeight > 0
        val bitmap = when {
            hasValidBounds -> {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
                }
                decodeBitmap(photoBytes, options)
            }

            else -> null
        }

        return bitmap?.let { decoded ->
            try {
                compress(decoded)
            } finally {
                decoded.recycle()
            }
        }
    }

    private fun decodeBitmap(
        photoBytes: ByteArray,
        options: BitmapFactory.Options,
    ): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size, options)
        } catch (e: OutOfMemoryError) {
            LogUtil.e(LogUtil.BUGLE_TAG, "Not enough memory to decode vCard photo", e)
            null
        }
    }

    private fun compress(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)

        return outputStream.toByteArray()
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var largestDimension = maxOf(width, height)

        while (largestDimension / 2 >= TARGET_DIMENSION_PX) {
            sampleSize *= 2
            largestDimension /= 2
        }

        return sampleSize
    }

    private companion object {
        private const val TARGET_DIMENSION_PX = 256
        private const val COMPRESSION_QUALITY = 85
    }
}
