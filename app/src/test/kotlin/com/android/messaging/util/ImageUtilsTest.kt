package com.android.messaging.util

import android.graphics.Bitmap
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
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
        val fixtureFile = File.createTempFile(
            "exif-orientation-fixture",
            ".jpg",
        )
        try {
            FileOutputStream(fixtureFile).use { outputStream ->
                val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
                val isCompressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                check(isCompressed) {
                    "Failed to compress JPEG fixture"
                }
            }
            ExifInterface(fixtureFile.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                saveAttributes()
            }
            return fixtureFile.readBytes()
        } finally {
            fixtureFile.delete()
        }
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
