package com.android.messaging.data.shareintent.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UriUtil
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface SharedAttachmentRepository {
    suspend fun persistToScratchSpace(
        sourceUri: Uri,
        contentType: String,
    ): ConversationDraftAttachment?
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
            val displayName = queryDisplayName(sourceUri)

            when (val persistedUri = UriUtil.persistContentToScratchSpace(sourceUri)) {
                null -> {
                    LogUtil.w(TAG, "Failed to persist shared attachment to scratch space")
                    null
                }

                else -> ConversationDraftAttachment(
                    contentType = contentType,
                    contentUri = persistedUri.toString(),
                    displayName = displayName,
                )
            }
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
        }.getOrNull()?.takeIf { name -> name.isNotBlank() }
    }

    private companion object {
        private const val TAG = "SharedAttachmentRepo"
    }
}
