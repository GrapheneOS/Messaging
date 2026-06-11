package com.android.messaging.data.media.repository.conversationattachments

import android.content.ContentResolver
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.PhotoPickerDraftAttachment
import com.android.messaging.data.media.model.AttachmentToSave
import com.android.messaging.data.media.model.PhotoPickerDraftAttachmentResult
import com.android.messaging.data.media.model.SaveAttachmentsResult
import com.android.messaging.data.media.repository.ConversationAttachmentsRepositoryImpl
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationAttachmentsRepositoryVideoMetadataTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun createDraftAttachmentsFromPhotoPicker_resolvesVideoMetadata() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val sourceUri = Uri.parse("content://picker/video")
            val scratchUri = Uri.parse("content://${MediaScratchFileProvider.AUTHORITY}/video")
            val scratchBytes = ByteArrayOutputStream()
            val contentResolver = createContentResolverForVideoPicker(
                sourceUri = sourceUri,
                contentType = "video/mp4",
                scratchUri = scratchUri,
                scratchSink = scratchBytes,
            )
            val repository = createRepository(contentResolver = contentResolver)

            mockScratchUri(scratchUri = scratchUri, extension = "mp4")
            mockVideoMetadata(
                contentResolver = contentResolver,
                scratchUri = scratchUri,
                width = "1920",
                height = "1080",
                durationMillis = "3000",
            )

            repository.createDraftAttachmentsFromPhotoPicker(
                contentUris = listOf(sourceUri.toString()),
            ).test {
                assertEquals(
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment = PhotoPickerDraftAttachment(
                            sourceContentUri = sourceUri.toString(),
                            draftAttachment = ConversationDraftAttachment(
                                contentType = "video/mp4",
                                contentUri = scratchUri.toString(),
                                width = 1920,
                                height = 1080,
                                durationMillis = 3000L,
                            ),
                        ),
                    ),
                    awaitItem(),
                )
                awaitComplete()
            }

            assertArrayEquals(byteArrayOf(1, 2, 3), scratchBytes.toByteArray())
            verify(exactly = 1) {
                anyConstructed<MediaMetadataRetriever>().release()
            }
        }
    }

    @Test
    fun createDraftAttachmentsFromPhotoPicker_continuesAfterUnresolvableItem() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val sourceUri = Uri.parse("https://example.test/video.mp4")
            val scratchUri = Uri.parse("content://${MediaScratchFileProvider.AUTHORITY}/video")
            val contentResolver = createContentResolverForVideoPicker(
                sourceUri = sourceUri,
                contentType = null,
                scratchUri = scratchUri,
            )
            val repository = createRepository(contentResolver = contentResolver)

            mockScratchUri(scratchUri = scratchUri, extension = "mp4")
            mockVideoMetadata(
                contentResolver = contentResolver,
                scratchUri = scratchUri,
                width = "640",
                height = "480",
                durationMillis = "not-a-number",
            )

            repository.createDraftAttachmentsFromPhotoPicker(
                contentUris = listOf("", sourceUri.toString()),
            ).test {
                assertEquals(
                    PhotoPickerDraftAttachmentResult.Failed(sourceContentUri = ""),
                    awaitItem(),
                )
                assertEquals(
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment = PhotoPickerDraftAttachment(
                            sourceContentUri = sourceUri.toString(),
                            draftAttachment = ConversationDraftAttachment(
                                contentType = "video/mp4",
                                contentUri = scratchUri.toString(),
                                width = 640,
                                height = 480,
                                durationMillis = null,
                            ),
                        ),
                    ),
                    awaitItem(),
                )
                awaitComplete()
            }
        }
    }

    @Test
    fun createDraftAttachmentsFromPhotoPicker_returnsVideoAttachmentWhenMetadataReadFails() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val sourceUri = Uri.parse("content://picker/video")
            val scratchUri = Uri.parse("content://${MediaScratchFileProvider.AUTHORITY}/video")
            val contentResolver = createContentResolverForVideoPicker(
                sourceUri = sourceUri,
                contentType = "video/mp4",
                scratchUri = scratchUri,
            )
            val repository = createRepository(contentResolver = contentResolver)

            mockScratchUri(scratchUri = scratchUri, extension = "mp4")
            mockkConstructor(MediaMetadataRetriever::class)
            every {
                contentResolver.openAssetFileDescriptor(scratchUri, "r")
            } throws IllegalStateException("metadata unavailable")
            every {
                anyConstructed<MediaMetadataRetriever>().release()
            } just runs

            repository.createDraftAttachmentsFromPhotoPicker(
                contentUris = listOf(sourceUri.toString()),
            ).test {
                assertEquals(
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment = PhotoPickerDraftAttachment(
                            sourceContentUri = sourceUri.toString(),
                            draftAttachment = ConversationDraftAttachment(
                                contentType = "video/mp4",
                                contentUri = scratchUri.toString(),
                                width = null,
                                height = null,
                                durationMillis = null,
                            ),
                        ),
                    ),
                    awaitItem(),
                )
                awaitComplete()
            }

            verify(exactly = 1) {
                anyConstructed<MediaMetadataRetriever>().release()
            }
        }
    }

    @Test
    fun saveAttachmentsToMediaStore_savesUnknownContentToDownloadsAndCountsAsOther() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val pendingUri = Uri.parse("content://media/external/downloads/pending")
            val sourceUri = Uri.parse("content://source/document.pdf")
            val sink = ByteArrayOutputStream()
            val contentResolver = mockk<ContentResolver>()
            val insertValues = slot<ContentValues>()
            every {
                contentResolver.insert(
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    capture(insertValues),
                )
            } returns pendingUri
            every { contentResolver.openInputStream(sourceUri) } returns
                ByteArrayInputStream(byteArrayOf(7, 8, 9))
            every { contentResolver.openOutputStream(pendingUri) } returns sink
            every { contentResolver.update(pendingUri, any(), null, null) } returns 1
            val repository = createRepository(contentResolver = contentResolver)

            repository.saveAttachmentsToMediaStore(
                attachments = listOf(
                    AttachmentToSave(
                        contentType = "application/pdf",
                        contentUri = sourceUri.toString(),
                    ),
                ),
            ).test {
                assertEquals(
                    SaveAttachmentsResult(
                        imageCount = 0,
                        videoCount = 0,
                        otherCount = 1,
                        failCount = 0,
                    ),
                    awaitItem(),
                )
                awaitComplete()
            }

            assertEquals(
                Environment.DIRECTORY_DOWNLOADS,
                insertValues.captured.getAsString(MediaStore.MediaColumns.RELATIVE_PATH),
            )
            assertEquals(
                "application/pdf",
                insertValues.captured.getAsString(MediaStore.MediaColumns.MIME_TYPE),
            )
            assertArrayEquals(byteArrayOf(7, 8, 9), sink.toByteArray())
        }
    }

    private fun createRepository(
        contentResolver: ContentResolver,
    ): ConversationAttachmentsRepositoryImpl {
        return ConversationAttachmentsRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun createContentResolverForVideoPicker(
        sourceUri: Uri,
        contentType: String?,
        scratchUri: Uri,
        scratchSink: ByteArrayOutputStream = ByteArrayOutputStream(),
    ): ContentResolver {
        val contentResolver = mockk<ContentResolver>()
        every { contentResolver.getType(sourceUri) } returns contentType
        every { contentResolver.openInputStream(sourceUri) } returns
            ByteArrayInputStream(byteArrayOf(1, 2, 3))
        every { contentResolver.openOutputStream(scratchUri) } returns scratchSink
        every { contentResolver.delete(scratchUri, null, null) } returns 1
        return contentResolver
    }

    @Suppress("SameParameterValue")
    private fun mockScratchUri(scratchUri: Uri, extension: String?) {
        mockkStatic(MediaScratchFileProvider::class)
        every {
            MediaScratchFileProvider.buildMediaScratchSpaceUri(extension)
        } returns scratchUri
    }

    private fun mockVideoMetadata(
        contentResolver: ContentResolver,
        scratchUri: Uri,
        width: String?,
        height: String?,
        durationMillis: String?,
    ) {
        val assetFileDescriptor = mockk<AssetFileDescriptor>()
        mockkConstructor(MediaMetadataRetriever::class)
        every { assetFileDescriptor.fileDescriptor } returns FileDescriptor()
        every { assetFileDescriptor.close() } just runs
        every {
            contentResolver.openAssetFileDescriptor(scratchUri, "r")
        } returns assetFileDescriptor
        every {
            anyConstructed<MediaMetadataRetriever>().setDataSource(any<FileDescriptor>())
        } just runs
        every {
            anyConstructed<MediaMetadataRetriever>()
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        } returns width
        every {
            anyConstructed<MediaMetadataRetriever>()
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        } returns height
        every {
            anyConstructed<MediaMetadataRetriever>()
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        } returns durationMillis
        every {
            anyConstructed<MediaMetadataRetriever>().release()
        } just runs
    }
}
