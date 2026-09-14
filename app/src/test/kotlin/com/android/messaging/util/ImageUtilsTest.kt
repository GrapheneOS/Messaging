package com.android.messaging.util

import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageUtilsTest {

    @Test
    fun getOrientation_readsOrientationValueFromJpegExif() {
        val jpeg = createJpegWithExifOrientation(orientation = ExifInterface.ORIENTATION_ROTATE_90)

        assertEquals(
            ExifInterface.ORIENTATION_ROTATE_90,
            ImageUtils.getOrientation(ByteArrayInputStream(jpeg)),
        )
    }

    @Test
    fun getOrientation_parsesExifFromStreamThatDoesNotSupportMark() {
        val jpeg = createJpegWithExifOrientation(orientation = ExifInterface.ORIENTATION_ROTATE_90)
        val streamWithoutMarkSupport = MarkUnsupportedInputStream(
            delegate = ByteArrayInputStream(jpeg),
        )

        assertEquals(
            ExifInterface.ORIENTATION_ROTATE_90,
            ImageUtils.getOrientation(streamWithoutMarkSupport),
        )
    }

    @Test
    fun getOrientation_returnsUndefinedForNonJpegStream() {
        val nonJpeg = ByteArrayInputStream(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))

        assertEquals(
            ExifInterface.ORIENTATION_UNDEFINED,
            ImageUtils.getOrientation(nonJpeg),
        )
    }

    @Test
    fun getOrientation_returnsUndefinedForNullStream() {
        assertEquals(
            ExifInterface.ORIENTATION_UNDEFINED,
            ImageUtils.getOrientation(null),
        )
    }

    private fun createJpegWithExifOrientation(orientation: Int): ByteArray {
        require(orientation in 0..UShort.MAX_VALUE.toInt()) {
            "EXIF orientation must fit in an unsigned short"
        }

        // Minimal JPEG containing a little-endian EXIF IFD with one orientation entry.
        return byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xE1.toByte(), 0x00, 0x22,
            0x45, 0x78, 0x69, 0x66, 0x00, 0x00,
            0x49, 0x49, 0x2A, 0x00,
            0x08, 0x00, 0x00, 0x00,
            0x01, 0x00,
            0x12, 0x01,
            0x03, 0x00,
            0x01, 0x00, 0x00, 0x00,
            (orientation and 0xFF).toByte(), (orientation ushr 8).toByte(), 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0xFF.toByte(), 0xD9.toByte(),
        )
    }

    private class MarkUnsupportedInputStream(
        private val delegate: InputStream,
    ) : InputStream() {

        override fun read(): Int {
            return delegate.read()
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            return delegate.read(buffer, offset, length)
        }
    }
}
