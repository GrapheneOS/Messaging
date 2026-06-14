package com.android.messaging.data.shareintent.repository

import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import com.android.messaging.util.MediaMetadataRetrieverWrapper
import com.android.messaging.util.UriUtil
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface SharedAttachmentRepository {
    suspend fun persistToScratchSpace(
        sourceUri: Uri,
        contentType: String,
    ): ConversationDraftAttachment?

    suspend fun readTextContent(sourceUri: Uri): String?
}

internal class SharedAttachmentRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : SharedAttachmentRepository {

    override suspend fun persistToScratchSpace(
        sourceUri: Uri,
        contentType: String,
    ): ConversationDraftAttachment? {
        return withContext(ioDispatcher) {
            if (UriUtil.isFileUri(sourceUri)) {
                LogUtil.i(TAG, "Ignoring shared attachment from an unsupported file URI")
                return@withContext null
            }

            val displayName = queryDisplayName(sourceUri)
            val durationMillis = audioDurationMillis(sourceUri, contentType)

            when (val persistedUri = UriUtil.persistContentToScratchSpace(sourceUri)) {
                null -> {
                    LogUtil.w(TAG, "Failed to persist shared attachment to scratch space")
                    null
                }

                else -> ConversationDraftAttachment(
                    contentType = contentType,
                    contentUri = persistedUri.toString(),
                    displayName = displayName,
                    durationMillis = durationMillis,
                )
            }
        }
    }

    override suspend fun readTextContent(sourceUri: Uri): String? {
        return withContext(ioDispatcher) {
            if (UriUtil.isFileUri(sourceUri)) {
                LogUtil.i(TAG, "Ignoring shared text from an unsupported file URI")
                return@withContext null
            }

            runCatching {
                contentResolver.openInputStream(sourceUri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
            }.onFailure {
                LogUtil.w(TAG, "Could not read shared text from $sourceUri", it)
            }.getOrNull()?.takeIf(String::isNotBlank)
        }
    }

    private fun audioDurationMillis(sourceUri: Uri, contentType: String): Long? {
        if (!ContentType.isAudioType(contentType)) {
            return null
        }

        val retriever = MediaMetadataRetrieverWrapper()
        return try {
            retriever.setDataSource(sourceUri)
            retriever.extractInteger(MediaMetadataRetriever.METADATA_KEY_DURATION, 0)
                .toLong()
                .takeIf { it > 0L }
        } catch (exception: IOException) {
            LogUtil.w(TAG, "Could not read duration of shared audio $sourceUri", exception)
            null
        } finally {
            retriever.release()
        }
    }

    private fun queryDisplayName(sourceUri: Uri): String? {
        return runCatching {
            contentResolver.query(
                sourceUri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                when {
                    cursor.moveToFirst() -> {
                        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        when {
                            columnIndex >= 0 -> cursor.getString(columnIndex)
                            else -> null
                        }
                    }

                    else -> null
                }
            }
        }.getOrNull()?.takeIf(String::isNotBlank)
    }

    private companion object {
        private const val TAG = "SharedAttachmentRepo"
    }
}
