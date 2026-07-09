@file:OptIn(ExperimentalCoroutinesApi::class)

package com.android.messaging.data.media.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.RemoteException
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.data.media.model.PhotoViewerItems
import com.android.messaging.data.media.model.PhotoViewerItemsLoadResult
import com.android.messaging.datamodel.ConversationImagePartsView
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.di.core.MessagingDbDispatcher
import com.android.messaging.domain.photoviewer.usecase.NormalizePhotoViewerUri
import com.android.messaging.util.LogUtil
import com.android.messaging.util.core.extension.typedFlow
import com.android.messaging.util.db.ext.getNonBlankStringOrNull
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface PhotoViewerRepository {
    fun getPhotoViewerItems(
        photosUri: Uri,
        initialPhotoUri: Uri,
        initialPhotoOccurrenceIndex: Int = 0,
    ): Flow<PhotoViewerItemsLoadResult>
}

internal class PhotoViewerRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val normalizePhotoViewerUri: NormalizePhotoViewerUri,
    @param:MessagingDbDispatcher
    private val messagingDbDispatcher: CoroutineDispatcher,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : PhotoViewerRepository {

    override fun getPhotoViewerItems(
        photosUri: Uri,
        initialPhotoUri: Uri,
        initialPhotoOccurrenceIndex: Int,
    ): Flow<PhotoViewerItemsLoadResult> {
        return observeUri(uri = photosUri)
            .flowOn(defaultDispatcher)
            .flatMapConcat {
                loadPhotoViewerItems(
                    photosUri = photosUri,
                    initialPhotoUri = initialPhotoUri,
                    initialPhotoOccurrenceIndex = initialPhotoOccurrenceIndex,
                ).recoverPhotoViewerItemsLoadFailure(photosUri = photosUri)
            }
            .recoverPhotoViewerItemsLoadFailure(photosUri = photosUri)
    }

    private fun loadPhotoViewerItems(
        photosUri: Uri,
        initialPhotoUri: Uri,
        initialPhotoOccurrenceIndex: Int,
    ): Flow<PhotoViewerItemsLoadResult> {
        return typedFlow { queryItems(photosUri = photosUri) }
            .flowOn(messagingDbDispatcher)
            .map { queriedItems ->
                val loadResult: PhotoViewerItemsLoadResult = PhotoViewerItemsLoadResult.Loaded(
                    photoViewerItems = buildPhotoViewerItems(
                        queriedItems = queriedItems,
                        initialPhotoUri = initialPhotoUri,
                        initialPhotoOccurrenceIndex = initialPhotoOccurrenceIndex,
                    ),
                )

                loadResult
            }
            .flowOn(defaultDispatcher)
    }

    private fun buildPhotoViewerItems(
        queriedItems: List<PhotoViewerItem>,
        initialPhotoUri: Uri,
        initialPhotoOccurrenceIndex: Int,
    ): PhotoViewerItems {
        val items = queriedItems.toImmutableList()

        return PhotoViewerItems(
            items = items,
            initialIndex = resolveInitialIndex(
                items = items,
                initialPhotoUri = initialPhotoUri,
                initialPhotoOccurrenceIndex = initialPhotoOccurrenceIndex,
            ),
        )
    }

    private fun observeUri(uri: Uri): Flow<Unit> {
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }

            contentResolver.registerContentObserver(uri, true, observer)
            trySend(Unit)

            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    private fun queryItems(photosUri: Uri): List<PhotoViewerItem> {
        return contentResolver
            .query(
                photosUri,
                ConversationImagePartsView.PhotoViewQuery.PROJECTION,
                null,
                null,
                null,
            )
            ?.use { cursor ->
                buildList(capacity = cursor.count) {
                    while (cursor.moveToNext()) {
                        val item = cursor.toPhotoViewerItem()
                        if (item != null) {
                            add(item)
                        }
                    }
                }
            }
            ?: emptyList()
    }

    private fun Cursor.toPhotoViewerItem(): PhotoViewerItem? {
        val contentUriString = getNonBlankStringOrNull(
            columnIndex = ConversationImagePartsView.PhotoViewQuery.INDEX_CONTENT_URI,
        )
        val contentType = getNonBlankStringOrNull(
            columnIndex = ConversationImagePartsView.PhotoViewQuery.INDEX_CONTENT_TYPE,
        )
        val status = getInt(ConversationImagePartsView.PhotoViewQuery.INDEX_STATUS)

        return when {
            contentUriString == null || contentType == null -> null
            else -> {
                val contentUri = contentUriString.toUri()

                PhotoViewerItem(
                    contentUri = contentUri,
                    contentType = contentType,
                    isIncoming = MessageData.getIsIncoming(status),
                    senderName = getStringOrNull(
                        ConversationImagePartsView
                            .PhotoViewQuery
                            .INDEX_SENDER_FULL_NAME,
                    ),
                    senderDestination = getStringOrNull(
                        ConversationImagePartsView
                            .PhotoViewQuery
                            .INDEX_DISPLAY_DESTINATION,
                    ),
                    receivedTimestampMillis = getLong(
                        ConversationImagePartsView
                            .PhotoViewQuery
                            .INDEX_RECEIVED_TIMESTAMP,
                    ),
                    isDraft = status == MessageData.BUGLE_STATUS_OUTGOING_DRAFT,
                    canUseActions = !MediaScratchFileProvider.isMediaScratchSpaceUri(contentUri),
                )
            }
        }
    }

    private fun resolveInitialIndex(
        items: List<PhotoViewerItem>,
        initialPhotoUri: Uri,
        initialPhotoOccurrenceIndex: Int,
    ): Int {
        val normalizedInitialPhotoUri = normalizePhotoViewerUri(uri = initialPhotoUri)
        val requestedOccurrenceIndex = initialPhotoOccurrenceIndex.coerceAtLeast(
            minimumValue = 0,
        )
        val matchingIndexes = items
            .mapIndexedNotNull { index, item ->
                when {
                    normalizePhotoViewerUri(
                        uri = item.contentUri,
                    ) == normalizedInitialPhotoUri -> index
                    else -> null
                }
            }
        val matchIndex = matchingIndexes.getOrNull(index = requestedOccurrenceIndex)
            ?: matchingIndexes.firstOrNull()
            ?: -1

        return matchIndex.coerceAtLeast(minimumValue = 0)
    }

    private fun Flow<PhotoViewerItemsLoadResult>.recoverPhotoViewerItemsLoadFailure(
        photosUri: Uri,
    ): Flow<PhotoViewerItemsLoadResult> {
        return catch { throwable ->
            when {
                isRecoverablePhotoViewerItemsLoadFailure(throwable = throwable) -> {
                    LogUtil.e(
                        TAG,
                        "Failed to load photo viewer items for $photosUri",
                        throwable,
                    )
                    emit(value = PhotoViewerItemsLoadResult.Error)
                }

                else -> throw throwable
            }
        }
    }

    private fun isRecoverablePhotoViewerItemsLoadFailure(throwable: Throwable): Boolean {
        return throwable is SQLiteException ||
            throwable is RemoteException ||
            throwable is SecurityException
    }

    private companion object {
        const val TAG = "PhotoViewerRepository"
    }
}
