package com.android.messaging.data.media.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.MatrixCursor
import android.database.sqlite.SQLiteException
import android.net.Uri
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.data.media.model.PhotoViewerItems
import com.android.messaging.data.media.model.PhotoViewerItemsLoadResult
import com.android.messaging.datamodel.ConversationImagePartsView
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.domain.photoviewer.usecase.NormalizePhotoViewerUriImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class PhotoViewerRepositoryImplTest {

    private val contentResolver = mockk<ContentResolver>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val repository = PhotoViewerRepositoryImpl(
        contentResolver = contentResolver,
        normalizePhotoViewerUri = NormalizePhotoViewerUriImpl(),
        messagingDbDispatcher = testDispatcher,
        defaultDispatcher = testDispatcher,
    )

    @Test
    fun getPhotoViewerItems_mapsRowsAndInitialIndex() {
        runTest(context = testDispatcher) {
            val cursor = MatrixCursor(ConversationImagePartsView.PhotoViewQuery.PROJECTION).apply {
                addPhotoRow(
                    uri = "content://example/photo/1",
                    senderName = "Ada",
                    contentUri = "content://example/content/1",
                    contentType = IMAGE_JPEG,
                    senderDestination = "+15550001",
                    receivedTimestampMillis = 1000L,
                    status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                )
                addPhotoRow(
                    uri = "content://example/photo/2",
                    senderName = null,
                    contentUri = "content://example/content/2?updated=true",
                    contentType = IMAGE_JPEG,
                    senderDestination = "+15550002",
                    receivedTimestampMillis = 2000L,
                    status = MessageData.BUGLE_STATUS_OUTGOING_DRAFT,
                )
            }
            stubContentResolver(cursor = cursor)

            val result = repository.getPhotoViewerItems(
                photosUri = photosUri,
                initialPhotoUri = Uri.parse("content://example/content/2"),
            ).firstLoadedForTest()

            assertEquals(1, result.initialIndex)
            assertEquals(2, result.items.size)
            assertEquals(true, result.items[0].isIncoming)
            assertEquals(
                PhotoViewerItem(
                    contentUri = Uri.parse("content://example/content/2?updated=true"),
                    contentType = IMAGE_JPEG,
                    isIncoming = false,
                    senderName = null,
                    senderDestination = "+15550002",
                    receivedTimestampMillis = 2000L,
                    isDraft = true,
                ),
                result.items[1],
            )
        }
    }

    @Test
    fun getPhotoViewerItems_whenInitialUriHasDuplicates_usesOccurrenceIndex() {
        runTest(context = testDispatcher) {
            val duplicateContentUri = "content://example/content/shared"
            val cursor = MatrixCursor(ConversationImagePartsView.PhotoViewQuery.PROJECTION).apply {
                addPhotoRow(
                    uri = "content://example/photo/1",
                    senderName = "Ada",
                    contentUri = duplicateContentUri,
                    contentType = IMAGE_JPEG,
                    senderDestination = "+15550001",
                    receivedTimestampMillis = 1000L,
                    status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                )
                addPhotoRow(
                    uri = "content://example/photo/2",
                    senderName = "Grace",
                    contentUri = duplicateContentUri,
                    contentType = IMAGE_JPEG,
                    senderDestination = "+15550002",
                    receivedTimestampMillis = 2000L,
                    status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                )
                addPhotoRow(
                    uri = "content://example/photo/3",
                    senderName = "Katherine",
                    contentUri = duplicateContentUri,
                    contentType = IMAGE_JPEG,
                    senderDestination = "+15550003",
                    receivedTimestampMillis = 3000L,
                    status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                )
            }
            stubContentResolver(cursor = cursor)

            val result = repository.getPhotoViewerItems(
                photosUri = photosUri,
                initialPhotoUri = Uri.parse(duplicateContentUri),
                initialPhotoOccurrenceIndex = 2,
            ).firstLoadedForTest()

            assertEquals(2, result.initialIndex)
        }
    }

    @Test
    fun getPhotoViewerItems_whenInitialMissing_usesZeroIndex() {
        runTest(context = testDispatcher) {
            val cursor = MatrixCursor(ConversationImagePartsView.PhotoViewQuery.PROJECTION).apply {
                addPhotoRow(
                    uri = "content://example/photo/1",
                    senderName = "Ada",
                    contentUri = "content://example/content/1",
                    contentType = IMAGE_JPEG,
                    senderDestination = "+15550001",
                    receivedTimestampMillis = 1000L,
                    status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                )
            }
            stubContentResolver(cursor = cursor)

            val result = repository.getPhotoViewerItems(
                photosUri = photosUri,
                initialPhotoUri = Uri.parse("content://example/missing"),
            ).firstLoadedForTest()

            assertEquals(0, result.initialIndex)
        }
    }

    @Test
    fun getPhotoViewerItems_whenContentUriIsScratchSpace_marksActionsUnavailable() {
        runTest(context = testDispatcher) {
            val scratchUri = Uri.parse(
                "content://${MediaScratchFileProvider.AUTHORITY}/800001?ext=jpg",
            )
            val cursor = MatrixCursor(ConversationImagePartsView.PhotoViewQuery.PROJECTION).apply {
                addPhotoRow(
                    uri = "content://example/photo/1",
                    senderName = "Ada",
                    contentUri = scratchUri.toString(),
                    contentType = IMAGE_JPEG,
                    senderDestination = "+15550001",
                    receivedTimestampMillis = 1000L,
                    status = MessageData.BUGLE_STATUS_OUTGOING_DRAFT,
                )
            }
            stubContentResolver(cursor = cursor)

            val result = repository.getPhotoViewerItems(
                photosUri = photosUri,
                initialPhotoUri = scratchUri,
            ).firstLoadedForTest()

            assertEquals(false, result.items[0].canUseActions)
        }
    }

    @Test
    fun getPhotoViewerItems_whenCursorIsNull_returnsEmptyLoadedResult() {
        runTest(context = testDispatcher) {
            stubContentResolver(cursor = null)

            val result = repository.getPhotoViewerItems(
                photosUri = photosUri,
                initialPhotoUri = Uri.parse("content://example/missing"),
            ).firstLoadedForTest()

            assertEquals(0, result.initialIndex)
            assertEquals(0, result.items.size)
        }
    }

    @Test
    fun getPhotoViewerItems_skipsRowsWithMissingRequiredValues() {
        runTest(context = testDispatcher) {
            val cursor = MatrixCursor(ConversationImagePartsView.PhotoViewQuery.PROJECTION).apply {
                addPhotoRow(
                    uri = null,
                    senderName = "Ada",
                    contentUri = "content://example/content/valid-without-legacy-uri",
                    contentType = IMAGE_JPEG,
                    senderDestination = "+15550001",
                    receivedTimestampMillis = 1000L,
                    status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                )
                addPhotoRow(
                    uri = "content://example/photo/invalid-content-uri",
                    senderName = "Invalid",
                    contentUri = null,
                    contentType = IMAGE_JPEG,
                    senderDestination = "+15550000",
                    receivedTimestampMillis = 1000L,
                    status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                )
                addPhotoRow(
                    uri = "content://example/photo/invalid-content-type",
                    senderName = "Invalid",
                    contentUri = "content://example/content/invalid-content-type",
                    contentType = null,
                    senderDestination = "+15550000",
                    receivedTimestampMillis = 1000L,
                    status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                )
                addPhotoRow(
                    uri = "content://example/photo/valid",
                    senderName = "Grace",
                    contentUri = "content://example/content/valid",
                    contentType = IMAGE_JPEG,
                    senderDestination = "+15550002",
                    receivedTimestampMillis = 2000L,
                    status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                )
            }
            stubContentResolver(cursor = cursor)

            val result = repository.getPhotoViewerItems(
                photosUri = photosUri,
                initialPhotoUri = Uri.parse("content://example/content/valid"),
            ).firstLoadedForTest()

            assertEquals(2, result.items.size)
            assertEquals(
                Uri.parse("content://example/content/valid-without-legacy-uri"),
                result.items[0].contentUri,
            )
            assertEquals(Uri.parse("content://example/content/valid"), result.items[1].contentUri)
            assertEquals(1, result.initialIndex)
        }
    }

    @Test
    fun getPhotoViewerItems_whenQueryThrows_returnsErrorResult() {
        runTest(context = testDispatcher) {
            every {
                contentResolver.query(
                    photosUri,
                    ConversationImagePartsView.PhotoViewQuery.PROJECTION,
                    null,
                    null,
                    null,
                )
            } throws SQLiteException("query failed")

            val result = repository.getPhotoViewerItems(
                photosUri = photosUri,
                initialPhotoUri = Uri.parse("content://example/missing"),
            ).first()

            assertTrue(result is PhotoViewerItemsLoadResult.Error)
        }
    }

    @Test
    fun getPhotoViewerItems_whenQueryFails_keepsObservingForRefresh() {
        runTest(context = testDispatcher) {
            val observerSlot = slot<ContentObserver>()
            val cursor = MatrixCursor(ConversationImagePartsView.PhotoViewQuery.PROJECTION).apply {
                addPhotoRow(
                    uri = "content://example/photo/1",
                    senderName = "Ada",
                    contentUri = "content://example/content/1",
                    contentType = IMAGE_JPEG,
                    senderDestination = "+15550001",
                    receivedTimestampMillis = 1000L,
                    status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                )
            }
            var queryCount = 0
            every {
                contentResolver.registerContentObserver(
                    photosUri,
                    true,
                    capture(observerSlot),
                )
            } returns Unit
            every {
                contentResolver.query(
                    photosUri,
                    ConversationImagePartsView.PhotoViewQuery.PROJECTION,
                    null,
                    null,
                    null,
                )
            } answers {
                queryCount += 1
                when (queryCount) {
                    1 -> throw SQLiteException("query failed")
                    else -> cursor
                }
            }

            val results = mutableListOf<PhotoViewerItemsLoadResult>()
            val collectJob = launch(testDispatcher) {
                repository
                    .getPhotoViewerItems(
                        photosUri = photosUri,
                        initialPhotoUri = Uri.parse("content://example/content/1"),
                    )
                    .take(count = 2)
                    .toList(destination = results)
            }

            runCurrent()
            observerSlot.captured.onChange(false)
            runCurrent()

            assertTrue(results[0] is PhotoViewerItemsLoadResult.Error)
            assertTrue(results[1] is PhotoViewerItemsLoadResult.Loaded)
            collectJob.cancel()
        }
    }

    @Test
    fun getPhotoViewerItems_whenUnexpectedRuntimeFailure_propagates() {
        assertThrows(IllegalStateException::class.java) {
            runTest(context = testDispatcher) {
                every {
                    contentResolver.query(
                        photosUri,
                        ConversationImagePartsView.PhotoViewQuery.PROJECTION,
                        null,
                        null,
                        null,
                    )
                } throws IllegalStateException("unexpected query failure")

                repository.getPhotoViewerItems(
                    photosUri = photosUri,
                    initialPhotoUri = Uri.parse("content://example/missing"),
                ).first()
            }
        }
    }

    private fun stubContentResolver(cursor: MatrixCursor?) {
        every {
            contentResolver.query(
                photosUri,
                ConversationImagePartsView.PhotoViewQuery.PROJECTION,
                null,
                null,
                null,
            )
        } returns cursor
    }

    private fun MatrixCursor.addPhotoRow(
        uri: String?,
        senderName: String?,
        contentUri: String?,
        contentType: String?,
        senderDestination: String,
        receivedTimestampMillis: Long,
        status: Int,
    ) {
        addRow(
            arrayOf<Any?>(
                uri,
                senderName,
                contentUri,
                null,
                contentType,
                senderDestination,
                receivedTimestampMillis,
                status,
            ),
        )
    }

    private suspend fun Flow<PhotoViewerItemsLoadResult>.firstLoadedForTest(): PhotoViewerItems {
        return when (val result = first()) {
            is PhotoViewerItemsLoadResult.Loaded -> result.photoViewerItems
            PhotoViewerItemsLoadResult.Error -> {
                throw AssertionError("Expected loaded photo viewer items")
            }
        }
    }

    private companion object {
        const val IMAGE_JPEG = "image/jpeg"

        val photosUri: Uri = Uri.parse("content://example/photos")
    }
}
