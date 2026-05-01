package com.android.messaging.data.media.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.ContactsContract.Contacts
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.PhotoPickerDraftAttachment
import com.android.messaging.data.media.model.AttachmentToSave
import com.android.messaging.data.media.model.PhotoPickerDraftAttachmentResult
import com.android.messaging.data.media.model.SaveAttachmentsResult
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal interface ConversationAttachmentsRepository {
    fun createDraftAttachmentsFromPhotoPicker(
        contentUris: List<String>,
    ): Flow<PhotoPickerDraftAttachmentResult>

    fun createDraftAttachmentFromContact(
        contactUri: String,
    ): Flow<ConversationDraftAttachment?>

    fun deleteTemporaryAttachment(
        contentUri: String,
    ): Flow<Unit>

    fun saveAttachmentsToMediaStore(
        attachments: List<AttachmentToSave>,
    ): Flow<SaveAttachmentsResult>
}

internal class ConversationAttachmentsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationAttachmentsRepository {

    @Suppress("TooGenericExceptionCaught")
    override fun createDraftAttachmentsFromPhotoPicker(
        contentUris: List<String>,
    ): Flow<PhotoPickerDraftAttachmentResult> {
        return flow {
            for (contentUri in contentUris) {
                val attachment = try {
                    createDraftAttachmentFromPhotoPicker(contentUri = contentUri)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LogUtil.w(TAG, "Failed to resolve photo picker attachment $contentUri", e)
                    null
                }

                val result = when (attachment) {
                    null -> {
                        PhotoPickerDraftAttachmentResult.Failed(
                            sourceContentUri = contentUri,
                        )
                    }

                    else -> {
                        PhotoPickerDraftAttachmentResult.Resolved(
                            photoPickerDraftAttachment = attachment,
                        )
                    }
                }

                emit(result)
            }
        }.flowOn(ioDispatcher)
    }

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
        attachments: List<AttachmentToSave>,
    ): Flow<SaveAttachmentsResult> {
        return typedFlow {
            saveAttachments(attachments = attachments)
        }.flowOn(ioDispatcher)
    }

    private fun saveAttachments(
        attachments: List<AttachmentToSave>,
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

    private fun createDraftAttachmentFromPhotoPicker(
        contentUri: String,
    ): PhotoPickerDraftAttachment? {
        val prepared = preparePhotoPickerContent(contentUri = contentUri)
            ?: return null

        val metadata = resolveVisualAttachmentMetadata(
            uri = prepared.scratchUri,
            contentType = prepared.contentType,
        )

        return PhotoPickerDraftAttachment(
            sourceContentUri = contentUri,
            draftAttachment = ConversationDraftAttachment(
                contentType = prepared.contentType,
                contentUri = prepared.scratchUri.toString(),
                width = metadata.width,
                height = metadata.height,
                durationMillis = metadata.durationMillis,
            ),
        )
    }

    private fun preparePhotoPickerContent(
        contentUri: String,
    ): PreparedPhotoPickerContent? {
        if (contentUri.isBlank()) {
            return null
        }

        val sourceUri = contentUri.toUri()
        val contentType = resolvePickerContentType(uri = sourceUri)
        val isVisualContent = ContentType.isImageType(contentType) ||
            ContentType.isVideoType(contentType)

        return when {
            !isVisualContent -> {
                LogUtil.w(TAG, "Dropping unsupported photo picker attachment $contentUri")
                null
            }

            else -> {
                copyPhotoPickerContentToScratchSpace(
                    sourceUri = sourceUri,
                    contentType = contentType,
                )?.let { scratchUri ->
                    PreparedPhotoPickerContent(
                        scratchUri = scratchUri,
                        contentType = contentType,
                    )
                }
            }
        }
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

    @Suppress("TooGenericExceptionCaught")
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
            copyUriContentOrThrow(
                sourceUri = sourceUri,
                targetUri = pendingUri,
            )
            true
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

    private fun copyPhotoPickerContentToScratchSpace(
        sourceUri: Uri,
        contentType: String,
    ): Uri? {
        val scratchUri = createScratchUri(contentType = contentType)
        val isCopied = copyPhotoPickerContent(
            sourceUri = sourceUri,
            scratchUri = scratchUri,
        )

        return when {
            isCopied -> scratchUri

            else -> {
                deleteScratchContent(scratchUri = scratchUri)
                null
            }
        }
    }

    private fun createScratchUri(contentType: String): Uri {
        return MimeTypeMap
            .getSingleton()
            .getExtensionFromMimeType(contentType)
            .let(MediaScratchFileProvider::buildMediaScratchSpaceUri)
    }

    private fun copyPhotoPickerContent(sourceUri: Uri, scratchUri: Uri): Boolean {
        return try {
            copyUriContentOrThrow(
                sourceUri = sourceUri,
                targetUri = scratchUri,
            )

            true
        } catch (e: IOException) {
            LogUtil.w(TAG, "Failed to copy photo picker content $sourceUri", e)
            false
        } catch (e: SecurityException) {
            LogUtil.w(TAG, "Permission denied while copying photo picker content $sourceUri", e)
            false
        }
    }

    private fun copyUriContentOrThrow(sourceUri: Uri, targetUri: Uri) {
        val sourceStream = contentResolver.openInputStream(sourceUri)
            ?: throw IOException("Unable to open input stream for $sourceUri")

        sourceStream.use { source ->
            val targetStream = contentResolver.openOutputStream(targetUri)
                ?: throw IOException("Unable to open output stream for $targetUri")

            targetStream.use(source::copyTo)
        }
    }

    private fun deleteScratchContent(scratchUri: Uri) {
        runCatching {
            contentResolver.delete(scratchUri, null, null)
        }
    }

    private fun resolvePickerContentType(uri: Uri): String {
        val contentType = contentResolver
            .getType(uri)
            ?.takeIf { it.isNotBlank() }

        if (contentType != null) {
            return contentType
        }

        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        val extensionContentType = MimeTypeMap
            .getSingleton()
            .getMimeTypeFromExtension(extension)
            ?.takeIf { it.isNotBlank() }

        return when {
            extensionContentType != null -> extensionContentType
            else -> ContentType.IMAGE_UNSPECIFIED
        }
    }

    private fun resolveVisualAttachmentMetadata(
        uri: Uri,
        contentType: String,
    ): VisualAttachmentMetadata {
        return when {
            ContentType.isVideoType(contentType) -> resolveVideoAttachmentMetadata(uri = uri)
            else -> resolveImageAttachmentMetadata(uri = uri)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun resolveImageAttachmentMetadata(uri: Uri): VisualAttachmentMetadata {
        val decodeBoundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeBoundsOptions)
            }
        } catch (e: Exception) {
            LogUtil.w(TAG, "Failed to decode photo picker image bounds for $uri", e)
        }

        return VisualAttachmentMetadata(
            width = decodeBoundsOptions.outWidth.takeIf { it > 0 },
            height = decodeBoundsOptions.outHeight.takeIf { it > 0 },
            durationMillis = null,
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun resolveVideoAttachmentMetadata(uri: Uri): VisualAttachmentMetadata {
        val retriever = MediaMetadataRetriever()

        return try {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { fileDescriptor ->
                retriever.setDataSource(fileDescriptor.fileDescriptor)
            }

            VisualAttachmentMetadata(
                width = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 },
                height = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 },
                durationMillis = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.takeIf { it > 0 },
            )
        } catch (e: Exception) {
            LogUtil.w(TAG, "Failed to decode photo picker video metadata for $uri", e)
            VisualAttachmentMetadata()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                LogUtil.w(TAG, "Failed to release media metadata retriever", e)
            }
        }
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

private data class PreparedPhotoPickerContent(
    val scratchUri: Uri,
    val contentType: String,
)

private data class VisualAttachmentMetadata(
    val width: Int? = null,
    val height: Int? = null,
    val durationMillis: Long? = null,
)

private enum class MediaKind { Image, Video, Audio, Other }

private data class MediaStoreTarget(
    val collection: Uri,
    val relativePath: String,
    val kind: MediaKind,
)
