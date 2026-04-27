package com.android.messaging.ui.conversation.v2.mediapicker.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.ContactsContract.Contacts
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import com.android.messaging.util.core.extension.typedFlow
import com.android.messaging.util.core.extension.unitFlow
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

internal interface ConversationAttachmentRepository {
    fun createDraftAttachmentFromContact(
        contactUri: String,
    ): Flow<ConversationDraftAttachment?>

    fun deleteTemporaryAttachment(
        contentUri: String,
    ): Flow<Unit>

    fun saveAttachmentsToMediaStore(
        attachments: List<AttachmentToSave>,
    ): Flow<SaveAttachmentsResult>

    data class AttachmentToSave(
        val contentType: String,
        val contentUri: String,
    )
}

internal class ConversationAttachmentRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationAttachmentRepository {

    override fun createDraftAttachmentFromContact(
        contactUri: String,
    ): Flow<ConversationDraftAttachment?> {
        return typedFlow {
            queryDraftAttachmentFromContact(contactUri = contactUri)
        }.catch { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }

            LogUtil.w(
                TAG,
                "Failed to resolve contact draft attachment for $contactUri",
                throwable,
            )
            emit(null)
        }.flowOn(ioDispatcher)
    }

    override fun deleteTemporaryAttachment(contentUri: String): Flow<Unit> {
        return unitFlow {
            val attachmentUri = contentUri.toUri()
            if (MediaScratchFileProvider.isMediaScratchSpaceUri(attachmentUri)) {
                contentResolver.delete(attachmentUri, null, null)
            }
        }.catch { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }

            LogUtil.w(TAG, "Failed to delete temporary attachment $contentUri", throwable)
            emit(Unit)
        }.flowOn(ioDispatcher)
    }

    override fun saveAttachmentsToMediaStore(
        attachments: List<ConversationAttachmentRepository.AttachmentToSave>,
    ): Flow<SaveAttachmentsResult> {
        return typedFlow {
            saveAttachments(attachments = attachments)
        }.flowOn(ioDispatcher)
    }

    private fun saveAttachments(
        attachments: List<ConversationAttachmentRepository.AttachmentToSave>,
    ): SaveAttachmentsResult {
        var imageCount = 0
        var videoCount = 0
        var otherCount = 0
        var failCount = 0

        for (attachment in attachments) {
            val target = mediaStoreTarget(contentType = attachment.contentType)
            val saved = saveOne(
                sourceUri = attachment.contentUri.toUri(),
                contentType = attachment.contentType,
                target = target,
            )

            if (!saved) {
                failCount++
                continue
            }

            when (target.kind) {
                MediaKind.Image -> imageCount++
                MediaKind.Video -> videoCount++
                MediaKind.Audio,
                MediaKind.Other,
                -> otherCount++
            }
        }

        return SaveAttachmentsResult(
            imageCount = imageCount,
            videoCount = videoCount,
            otherCount = otherCount,
            failCount = failCount,
        )
    }

    private fun saveOne(
        sourceUri: Uri,
        contentType: String,
        target: MediaStoreTarget,
    ): Boolean {
        val pendingUri = insertPendingRow(
            contentType = contentType,
            target = target,
        ) ?: return false

        val copied = copyToPending(
            sourceUri = sourceUri,
            pendingUri = pendingUri,
        )

        if (copied) {
            finalizePendingRow(pendingUri = pendingUri)
        } else {
            deletePendingRow(pendingUri = pendingUri)
        }

        return copied
    }

    private fun insertPendingRow(
        contentType: String,
        target: MediaStoreTarget,
    ): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, buildDisplayName(contentType = contentType))
            put(MediaStore.MediaColumns.MIME_TYPE, contentType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, target.relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        return try {
            contentResolver.insert(target.collection, values)
        } catch (e: Exception) {
            LogUtil.e(TAG, "MediaStore insert failed for $contentType", e)
            null
        }
    }

    private fun copyToPending(
        sourceUri: Uri,
        pendingUri: Uri,
    ): Boolean {
        return try {
            contentResolver.openInputStream(sourceUri)?.use { source ->
                contentResolver.openOutputStream(pendingUri)?.use { sink ->
                    source.copyTo(sink)
                    true
                }
            } ?: false
        } catch (e: IOException) {
            LogUtil.e(TAG, "Copy to MediaStore failed for $sourceUri", e)
            false
        } catch (e: SecurityException) {
            LogUtil.e(TAG, "Copy to MediaStore denied for $sourceUri", e)
            false
        }
    }

    private fun buildDisplayName(contentType: String): String {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType) ?: "bin"
        return "${System.currentTimeMillis()}.$extension"
    }

    private fun mediaStoreTarget(contentType: String): MediaStoreTarget {
        val volume = MediaStore.VOLUME_EXTERNAL_PRIMARY

        return when {
            ContentType.isImageType(contentType) -> MediaStoreTarget(
                collection = MediaStore.Images.Media.getContentUri(volume),
                relativePath = Environment.DIRECTORY_PICTURES + "/" + SAVED_ATTACHMENTS_FOLDER,
                kind = MediaKind.Image,
            )

            ContentType.isVideoType(contentType) -> MediaStoreTarget(
                collection = MediaStore.Video.Media.getContentUri(volume),
                relativePath = Environment.DIRECTORY_PICTURES + "/" + SAVED_ATTACHMENTS_FOLDER,
                kind = MediaKind.Video,
            )

            ContentType.isAudioType(contentType) -> MediaStoreTarget(
                collection = MediaStore.Audio.Media.getContentUri(volume),
                relativePath = Environment.DIRECTORY_MUSIC + "/" + SAVED_ATTACHMENTS_FOLDER,
                kind = MediaKind.Audio,
            )

            else -> MediaStoreTarget(
                collection = MediaStore.Downloads.getContentUri(volume),
                relativePath = Environment.DIRECTORY_DOWNLOADS,
                kind = MediaKind.Other,
            )
        }
    }

    private fun deletePendingRow(pendingUri: Uri) {
        runCatching { contentResolver.delete(pendingUri, null, null) }
    }

    private fun finalizePendingRow(pendingUri: Uri) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        runCatching { contentResolver.update(pendingUri, values, null, null) }
    }

    private fun queryDraftAttachmentFromContact(
        contactUri: String,
    ): ConversationDraftAttachment? {
        val lookupKey = contentResolver.query(
            contactUri.toUri(),
            arrayOf(Contacts.LOOKUP_KEY),
            null,
            null,
            null,
        )?.use { cursor ->
            val lookupKeyColumnIndex = cursor.getColumnIndexOrThrow(Contacts.LOOKUP_KEY)

            when {
                cursor.moveToFirst() -> cursor.getStringOrNull(lookupKeyColumnIndex)
                else -> null
            }
        }

        if (lookupKey.isNullOrBlank()) {
            LogUtil.w(TAG, "Unable to resolve contact lookup key for $contactUri")
            return null
        }

        val vCardUri = Uri.withAppendedPath(
            Contacts.CONTENT_VCARD_URI,
            lookupKey,
        )

        return ConversationDraftAttachment(
            contentType = ContentType.TEXT_VCARD,
            contentUri = vCardUri.toString(),
        )
    }

    private companion object {
        private const val TAG = "ConversationAttachmentRepository"

        private const val SAVED_ATTACHMENTS_FOLDER = "Messaging"
    }
}

private enum class MediaKind { Image, Video, Audio, Other }

private data class MediaStoreTarget(
    val collection: Uri,
    val relativePath: String,
    val kind: MediaKind,
)
