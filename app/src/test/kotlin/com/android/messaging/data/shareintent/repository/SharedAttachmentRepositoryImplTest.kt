package com.android.messaging.data.shareintent.repository

import android.content.ContentResolver
import android.database.Cursor
import android.database.MatrixCursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.android.messaging.util.ContentType
import com.android.messaging.util.MediaMetadataRetrieverWrapper
import com.android.messaging.util.UriUtil
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class SharedAttachmentRepositoryImplTest {

    private val contentResolver = mockk<ContentResolver>()
    private val testDispatcher = StandardTestDispatcher()

    private val repository = SharedAttachmentRepositoryImpl(
        contentResolver = contentResolver,
        ioDispatcher = testDispatcher,
    )

    @Before
    fun setUp() {
        mockkStatic(UriUtil::class)
        every { UriUtil.isFileUri(any()) } returns false
        every { UriUtil.persistContentToScratchSpace(any<Uri>()) } returns PERSISTED_URI
        stubQuery(cursor = null)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun persistToScratchSpace_fileUri_returnsNullWithoutPersisting() = runTest(testDispatcher) {
        val sourceUri = Uri.parse("file:///sdcard/photo.jpg")
        every { UriUtil.isFileUri(sourceUri) } returns true

        val result = repository.persistToScratchSpace(sourceUri, IMAGE_TYPE)

        assertNull(result)
        verify(exactly = 0) { UriUtil.persistContentToScratchSpace(any<Uri>()) }
    }

    @Test
    fun persistToScratchSpace_persistFails_returnsNull() = runTest(testDispatcher) {
        every { UriUtil.persistContentToScratchSpace(SOURCE_URI) } returns null

        val result = repository.persistToScratchSpace(SOURCE_URI, IMAGE_TYPE)

        assertNull(result)
    }

    @Test
    fun persistToScratchSpace_image_returnsAttachmentWithoutNameOrDuration() = runTest(
        testDispatcher,
    ) {
        val result = repository.persistToScratchSpace(SOURCE_URI, IMAGE_TYPE)

        assertEquals(IMAGE_TYPE, result?.contentType)
        assertEquals(PERSISTED_URI.toString(), result?.contentUri)
        assertNull(result?.displayName)
        assertNull(result?.durationMillis)
    }

    @Test
    fun persistToScratchSpace_displayNameAvailable_setsDisplayName() = runTest(testDispatcher) {
        stubQuery(displayNameCursor("photo.jpg"))

        val result = repository.persistToScratchSpace(SOURCE_URI, IMAGE_TYPE)

        assertEquals("photo.jpg", result?.displayName)
    }

    @Test
    fun persistToScratchSpace_blankDisplayName_returnsNullDisplayName() = runTest(testDispatcher) {
        stubQuery(displayNameCursor("   "))

        val result = repository.persistToScratchSpace(SOURCE_URI, IMAGE_TYPE)

        assertNull(result?.displayName)
    }

    @Test
    fun persistToScratchSpace_missingColumn_returnsNullDisplayName() = runTest(testDispatcher) {
        stubQuery(MatrixCursor(arrayOf("other")).apply { addRow(arrayOf<Any?>("value")) })

        val result = repository.persistToScratchSpace(SOURCE_URI, IMAGE_TYPE)

        assertNull(result?.displayName)
    }

    @Test
    fun persistToScratchSpace_emptyCursor_returnsNullDisplayName() = runTest(testDispatcher) {
        stubQuery(MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)))

        val result = repository.persistToScratchSpace(SOURCE_URI, IMAGE_TYPE)

        assertNull(result?.displayName)
    }

    @Test
    fun persistToScratchSpace_queryThrows_returnsNullDisplayName() = runTest(testDispatcher) {
        every {
            contentResolver.query(any(), any(), any(), any(), any())
        } throws SecurityException("denied")

        val result = repository.persistToScratchSpace(SOURCE_URI, IMAGE_TYPE)

        assertNull(result?.displayName)
    }

    @Test
    fun persistToScratchSpace_audioWithDuration_setsDurationMillis() = runTest(testDispatcher) {
        stubAudioDuration(durationMillis = 4200)

        val result = repository.persistToScratchSpace(SOURCE_URI, AUDIO_TYPE)

        assertEquals(4200L, result?.durationMillis)
    }

    @Test
    fun persistToScratchSpace_audioNonPositiveDuration_returnsNullDuration() = runTest(
        testDispatcher,
    ) {
        stubAudioDuration(durationMillis = 0)

        val result = repository.persistToScratchSpace(SOURCE_URI, AUDIO_TYPE)

        assertNull(result?.durationMillis)
    }

    @Test
    fun persistToScratchSpace_audioUnreadable_returnsNullDuration() = runTest(testDispatcher) {
        stubAudioReadFailure()

        val result = repository.persistToScratchSpace(SOURCE_URI, AUDIO_TYPE)

        assertEquals(AUDIO_TYPE, result?.contentType)
        assertNull(result?.durationMillis)
    }

    private fun stubQuery(cursor: Cursor?) {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns cursor
    }

    private fun displayNameCursor(displayName: String): Cursor {
        return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply {
            addRow(arrayOf<Any?>(displayName))
        }
    }

    private fun stubAudioDuration(durationMillis: Int) {
        mockkConstructor(MediaMetadataRetrieverWrapper::class)
        every { anyConstructed<MediaMetadataRetrieverWrapper>().setDataSource(any()) } just Runs
        every {
            anyConstructed<MediaMetadataRetrieverWrapper>()
                .extractInteger(MediaMetadataRetriever.METADATA_KEY_DURATION, 0)
        } returns durationMillis
        every { anyConstructed<MediaMetadataRetrieverWrapper>().release() } just Runs
    }

    private fun stubAudioReadFailure() {
        mockkConstructor(MediaMetadataRetrieverWrapper::class)
        every {
            anyConstructed<MediaMetadataRetrieverWrapper>().setDataSource(any())
        } throws IOException("unreadable")
        every { anyConstructed<MediaMetadataRetrieverWrapper>().release() } just Runs
    }

    private companion object {
        private val SOURCE_URI: Uri = Uri.parse("content://media/external/images/1")
        private val PERSISTED_URI: Uri = Uri.parse("content://scratch/1")
        private const val IMAGE_TYPE = ContentType.IMAGE_JPEG
        private const val AUDIO_TYPE = ContentType.AUDIO_MP3
    }
}
