package com.android.messaging.ui.conversation.attachment.ui.thumbnail

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.ui.unit.IntSize
import com.android.messaging.ui.common.components.attachment.loadMediaThumbnailBitmap
import com.android.messaging.util.MediaMetadataRetrieverWrapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaThumbnailBitmapLoaderTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun loadBitmap_returnsPlatformThumbnailWhenAvailable() {
        runTest {
            val contentResolver = mockk<ContentResolver>(relaxed = true)
            val contentUri = Uri.parse("content://media/image/1")
            val platformBitmap = createBitmap(width = 24, height = 12)
            val capturedSize = slot<Size>()
            every {
                contentResolver.loadThumbnail(contentUri, capture(capturedSize), null)
            } returns platformBitmap

            val result = loadMediaThumbnailBitmap(
                contentResolver = contentResolver,
                contentUri = contentUri,
                contentType = "image/png",
                size = IntSize(width = 64, height = 32),
                softenBitmap = false,
            )

            assertSame(platformBitmap, result)
            assertEquals(Size(64, 32), capturedSize.captured)
            verify(exactly = 0) {
                contentResolver.openInputStream(any())
            }
        }
    }

    @Test
    fun loadBitmap_fallsBackToImageDecodeWhenPlatformThumbnailFails() {
        runTest {
            val contentResolver = mockk<ContentResolver>()
            val contentUri = Uri.parse("content://media/image/2")
            val imageBytes = createPngBytes(width = 80, height = 40)
            every {
                contentResolver.loadThumbnail(contentUri, any(), null)
            } throws IOException("thumbnail unavailable")
            every {
                contentResolver.openInputStream(contentUri)
            } answers {
                ByteArrayInputStream(imageBytes)
            }

            val result = loadMediaThumbnailBitmap(
                contentResolver = contentResolver,
                contentUri = contentUri,
                contentType = "image/png",
                size = IntSize(width = 20, height = 20),
                softenBitmap = false,
            )

            assertNotNull(result)
            assertEquals(40, result?.width)
            assertEquals(20, result?.height)
            verify(exactly = 2) {
                contentResolver.openInputStream(contentUri)
            }
        }
    }

    @Test
    fun loadBitmap_fallsBackToVideoFrameAndScalesDown() {
        runTest {
            val contentResolver = mockk<ContentResolver>()
            val contentUri = Uri.parse("content://media/video/1")
            val videoFrame = createBitmap(width = 200, height = 100)
            every {
                contentResolver.loadThumbnail(contentUri, any(), null)
            } throws IOException("thumbnail unavailable")
            mockkConstructor(MediaMetadataRetrieverWrapper::class)
            every {
                anyConstructed<MediaMetadataRetrieverWrapper>().setDataSource(contentUri)
            } just runs
            every {
                anyConstructed<MediaMetadataRetrieverWrapper>().frameAtTime
            } returns videoFrame
            every {
                anyConstructed<MediaMetadataRetrieverWrapper>().release()
            } just runs

            val result = loadMediaThumbnailBitmap(
                contentResolver = contentResolver,
                contentUri = contentUri,
                contentType = "video/mp4",
                size = IntSize(width = 50, height = 50),
                softenBitmap = false,
            )

            assertNotSame(videoFrame, result)
            assertEquals(50, result?.width)
            assertEquals(25, result?.height)
            verify(exactly = 1) {
                anyConstructed<MediaMetadataRetrieverWrapper>().release()
            }
        }
    }

    @Test
    fun loadBitmap_returnsNullForUnsupportedContentWhenThumbnailUnavailable() {
        runTest {
            val contentResolver = mockk<ContentResolver>(relaxed = true)
            val contentUri = Uri.parse("content://media/file/1")
            every {
                contentResolver.loadThumbnail(contentUri, any(), null)
            } throws IOException("thumbnail unavailable")

            val result = loadMediaThumbnailBitmap(
                contentResolver = contentResolver,
                contentUri = contentUri,
                contentType = "application/pdf",
                size = IntSize(width = 32, height = 32),
                softenBitmap = false,
            )

            assertNull(result)
            verify(exactly = 0) {
                contentResolver.openInputStream(any())
            }
        }
    }

    @Test
    fun loadBitmap_softensThumbnailIntoRequestedOutputSize() {
        runTest {
            val contentResolver = mockk<ContentResolver>()
            val contentUri = Uri.parse("content://media/image/3")
            val platformBitmap = createBitmap(width = 80, height = 20)
            every {
                contentResolver.loadThumbnail(contentUri, any(), null)
            } returns platformBitmap

            val result = loadMediaThumbnailBitmap(
                contentResolver = contentResolver,
                contentUri = contentUri,
                contentType = "image/png",
                size = IntSize(width = 60, height = 30),
                softenBitmap = true,
            )

            assertNotSame(platformBitmap, result)
            assertEquals(60, result?.width)
            assertEquals(30, result?.height)
        }
    }

    private fun createBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    @Suppress("SameParameterValue")
    private fun createPngBytes(width: Int, height: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        createBitmap(width = width, height = height)
            .compress(Bitmap.CompressFormat.PNG, 100, stream)

        return stream.toByteArray()
    }
}
