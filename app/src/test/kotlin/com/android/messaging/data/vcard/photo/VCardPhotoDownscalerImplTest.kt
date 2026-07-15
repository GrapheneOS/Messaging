package com.android.messaging.data.vcard.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val MAX_EXPECTED_DIMENSION = 512

@RunWith(RobolectricTestRunner::class)
internal class VCardPhotoDownscalerImplTest {

    private val downscaler = VCardPhotoDownscalerImpl()

    @Test
    fun downscale_emptyBytes_returnsNull() {
        assertNull(downscaler.downscale(ByteArray(0)))
    }

    @Test
    fun downscale_smallImage_returnsDecodableImage() {
        val result = downscaler.downscale(
            pngBytes(
                width = 64,
                height = 64,
            ),
        )

        val bitmap = decode(result)
        assertTrue(bitmap.width in 1..MAX_EXPECTED_DIMENSION)
    }

    @Test
    fun downscale_largeImage_shrinksBelowTarget() {
        val result = downscaler.downscale(
            pngBytes(
                width = 2048,
                height = 1024,
            ),
        )

        val bitmap = decode(result)
        assertTrue(bitmap.width <= MAX_EXPECTED_DIMENSION)
        assertTrue(bitmap.height <= MAX_EXPECTED_DIMENSION)
    }

    private fun pngBytes(
        width: Int,
        height: Int,
    ): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        bitmap.recycle()

        return outputStream.toByteArray()
    }

    private fun decode(bytes: ByteArray?): Bitmap {
        requireNotNull(bytes)

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
