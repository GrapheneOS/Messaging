package com.android.messaging.data.media.repository

import android.content.ContentResolver
import android.os.Bundle
import android.provider.MediaStore
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.ContentType
import com.android.messaging.util.UriUtil
import com.android.messaging.util.core.extension.typedFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

internal interface ConversationMediaRepository {
    fun getRecentMedia(limit: Int = DEFAULT_RECENT_MEDIA_LIMIT): Flow<List<ConversationMediaItem>>

    private companion object {
        private const val DEFAULT_RECENT_MEDIA_LIMIT = 200
    }
}

internal class ConversationMediaRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationMediaRepository {

    override fun getRecentMedia(limit: Int): Flow<List<ConversationMediaItem>> {
        return typedFlow {
            queryRecentMedia(limit = limit)
        }.flowOn(context = ioDispatcher)
    }

    private fun queryRecentMedia(limit: Int): List<ConversationMediaItem> {
        return contentResolver.query(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            RECENT_MEDIA_PROJECTION,
            createRecentMediaQueryArgs(limit = limit),
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mediaTypeIndex = cursor.getColumnIndexOrThrow(
                MediaStore.Files.FileColumns.MEDIA_TYPE,
            )
            val mimeTypeIndex = cursor.getColumnIndexOrThrow(
                MediaStore.Files.FileColumns.MIME_TYPE,
            )
            val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightIndex = cursor.getColumnIndexOrThrow(
                MediaStore.Files.FileColumns.HEIGHT,
            )
            val durationIndex = cursor.getColumnIndexOrThrow(
                MediaStore.Video.VideoColumns.DURATION,
            )

            buildList(capacity = cursor.count) {
                while (cursor.moveToNext()) {
                    val mediaStoreId = cursor.getLong(idIndex)
                    val mediaTypeValue = cursor.getInt(mediaTypeIndex)

                    val item = ConversationMediaItem(
                        mediaId = mediaStoreId.toString(),
                        contentUri = UriUtil
                            .getContentUriForMediaStoreId(mediaStoreId)
                            .toString(),
                        contentType = cursor
                            .getString(mimeTypeIndex)
                            ?.takeIf { it.isNotBlank() }
                            ?: fallbackContentType(mediaTypeValue = mediaTypeValue),
                        width = cursor.getInt(widthIndex).takeIf { it > 0 },
                        height = cursor.getInt(heightIndex).takeIf { it > 0 },
                        durationMillis = cursor.getLong(durationIndex).takeIf { it > 0 },
                    )

                    add(item)
                }
            }
        }.orEmpty()
    }

    private fun createRecentMediaQueryArgs(limit: Int): Bundle {
        return Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                RECENT_MEDIA_SELECTION,
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Files.FileColumns.DATE_ADDED),
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        }
    }

    private fun fallbackContentType(mediaTypeValue: Int): String {
        return when (mediaTypeValue) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> ContentType.VIDEO_UNSPECIFIED
            else -> ContentType.IMAGE_UNSPECIFIED
        }
    }

    private companion object {
        private val RECENT_MEDIA_PROJECTION: Array<String> = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Video.VideoColumns.DURATION,
        )

        private val RECENT_MEDIA_SELECTION: String by lazy {
            buildString {
                append(MediaStore.Files.FileColumns.MEDIA_TYPE)
                append(" IN (")
                append(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
                append(",")
                append(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                append(")")
            }
        }
    }
}
