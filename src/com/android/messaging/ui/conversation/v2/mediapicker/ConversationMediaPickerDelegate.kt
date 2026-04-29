package com.android.messaging.ui.conversation.v2.mediapicker

import com.android.messaging.R
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.mapper.ConversationDraftAttachmentMapper
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.v2.mediapicker.model.PhotoPickerDraftAttachment
import com.android.messaging.ui.conversation.v2.mediapicker.model.PhotoPickerDraftAttachmentResult
import com.android.messaging.ui.conversation.v2.mediapicker.repository.ConversationAttachmentRepository
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.util.LogUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import javax.inject.Inject

internal interface ConversationMediaPickerDelegate {
    val effects: Flow<ConversationScreenEffect>
    val photoPickerSourceContentUriByAttachmentContentUri: StateFlow<ImmutableMap<String, String>>

    fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    )

    fun onPhotoPickerMediaSelected(contentUris: List<String>)

    fun onPhotoPickerMediaDeselected(contentUris: List<String>)

    fun onCapturedMediaReady(capturedMedia: ConversationCapturedMedia)

    fun onContactCardPicked(contactUri: String?)

    fun onRemovePendingAttachment(pendingAttachmentId: String)

    fun onRemoveResolvedAttachment(contentUri: String)

    fun onScreenCleared()
}

internal class ConversationMediaPickerDelegateImpl @Inject constructor(
    private val conversationDraftDelegate: ConversationDraftDelegate,
    private val conversationAttachmentRepository: ConversationAttachmentRepository,
    private val conversationDraftAttachmentMapper: ConversationDraftAttachmentMapper,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationMediaPickerDelegate {

    private val _effects = MutableSharedFlow<ConversationScreenEffect>(
        extraBufferCapacity = 1,
    )
    private val photoPickerAttachmentLock = Any()

    private val pendingAttachmentJobs = mutableMapOf<String, Job>()
    private val photoPickerContentUris = mutableSetOf<String>()
    private val attachmentContentUriByPhotoPickerContentUri = mutableMapOf<String, String>()
    private val photoPickerContentUriByAttachmentContentUri = mutableMapOf<String, String>()
    private val _photoPickerSourceContentUriByAttachmentContentUri =
        MutableStateFlow<ImmutableMap<String, String>>(persistentMapOf())

    override val effects = _effects.asSharedFlow()
    override val photoPickerSourceContentUriByAttachmentContentUri =
        _photoPickerSourceContentUriByAttachmentContentUri.asStateFlow()

    private var boundScope: CoroutineScope? = null

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        if (boundScope != null) {
            return
        }

        boundScope = scope

        scope.launch(defaultDispatcher) {
            conversationIdFlow
                .drop(count = 1)
                .collect {
                    cancelPendingAttachmentJobs()
                }
        }
    }

    override fun onPhotoPickerMediaSelected(contentUris: List<String>) {
        claimNewPhotoPickerContentUris(contentUris = contentUris)
            .takeIf { it.isNotEmpty() }
            ?.let(::launchPhotoPickerAttachmentResolution)
    }

    private fun claimNewPhotoPickerContentUris(contentUris: List<String>): List<String> {
        return synchronized(photoPickerAttachmentLock) {
            contentUris.filter { contentUri ->
                contentUri.isNotBlank() && photoPickerContentUris.add(contentUri)
            }
        }
    }

    private fun launchPhotoPickerAttachmentResolution(contentUris: List<String>) {
        boundScope?.launch(defaultDispatcher) {
            conversationAttachmentRepository
                .createDraftAttachmentsFromPhotoPicker(contentUris = contentUris)
                .catch { throwable ->
                    handlePhotoPickerAttachmentResolutionException(
                        contentUris = contentUris,
                        throwable = throwable,
                    )
                }
                .collect { result ->
                    handlePhotoPickerAttachmentResult(result = result)
                }
        }
    }

    private suspend fun handlePhotoPickerAttachmentResolutionException(
        contentUris: List<String>,
        throwable: Throwable,
    ) {
        if (throwable is CancellationException) {
            throw throwable
        }

        LogUtil.w(TAG, "Unable to resolve photo picker attachments", throwable)

        releasePhotoPickerContentUris(contentUris = contentUris)
        emitAttachmentLoadFailedEffect()
    }

    private suspend fun handlePhotoPickerAttachmentResult(
        result: PhotoPickerDraftAttachmentResult,
    ) {
        when (result) {
            is PhotoPickerDraftAttachmentResult.Resolved -> {
                onPhotoPickerAttachmentResolved(result.photoPickerDraftAttachment)
            }

            is PhotoPickerDraftAttachmentResult.Failed -> {
                val wasSelected = releasePhotoPickerContentUri(result.sourceContentUri)

                if (wasSelected) {
                    emitAttachmentLoadFailedEffect()
                }
            }
        }
    }

    private fun onPhotoPickerAttachmentResolved(
        photoPickerAttachment: PhotoPickerDraftAttachment,
    ) {
        val shouldDeleteTemporaryAttachment = synchronized(photoPickerAttachmentLock) {
            val sourceContentUri = photoPickerAttachment.sourceContentUri
            if (!photoPickerContentUris.contains(sourceContentUri)) {
                return@synchronized true
            }

            registerPhotoPickerAttachment(photoPickerAttachment)
            conversationDraftDelegate.addAttachments(
                attachments = listOf(
                    photoPickerAttachment.draftAttachment,
                ),
            )

            false
        }

        if (shouldDeleteTemporaryAttachment) {
            deleteTemporaryAttachment(
                contentUri = photoPickerAttachment.draftAttachment.contentUri,
            )
        }
    }

    private fun releasePhotoPickerContentUri(contentUri: String): Boolean {
        return synchronized(photoPickerAttachmentLock) {
            photoPickerContentUris.remove(contentUri)
        }
    }

    private fun releasePhotoPickerContentUris(contentUris: List<String>) {
        synchronized(photoPickerAttachmentLock) {
            photoPickerContentUris.removeAll(contentUris.toSet())
        }
    }

    private suspend fun emitAttachmentLoadFailedEffect() {
        _effects.emit(
            ConversationScreenEffect.ShowMessage(
                messageResId = R.string.fail_to_load_attachment,
            ),
        )
    }

    override fun onPhotoPickerMediaDeselected(contentUris: List<String>) {
        contentUris
            .filter { it.isNotBlank() }
            .forEach { photoPickerContentUri ->
                val attachmentContentUri = synchronized(photoPickerAttachmentLock) {
                    val registeredContentUri = unregisterPhotoPickerAttachmentByPickerUri(
                        photoPickerContentUri = photoPickerContentUri,
                    )

                    photoPickerContentUris.remove(photoPickerContentUri)

                    registeredContentUri
                } ?: photoPickerContentUri

                conversationDraftDelegate.removeAttachment(attachmentContentUri)
                deleteTemporaryAttachment(attachmentContentUri)
            }
    }

    override fun onCapturedMediaReady(capturedMedia: ConversationCapturedMedia) {
        conversationDraftDelegate.addAttachments(
            attachments = listOf(
                conversationDraftAttachmentMapper.map(
                    capturedMedia = capturedMedia,
                ),
            ),
        )
    }

    override fun onContactCardPicked(contactUri: String?) {
        val resolvedContactUri = contactUri?.takeIf { it.isNotBlank() } ?: return

        boundScope?.launch(defaultDispatcher) {
            conversationAttachmentRepository
                .createDraftAttachmentFromContact(contactUri = resolvedContactUri)
                .filterNotNull()
                .map(::listOf)
                .collect(conversationDraftDelegate::addAttachments)
        }
    }

    override fun onRemovePendingAttachment(pendingAttachmentId: String) {
        synchronized(photoPickerAttachmentLock) {
            pendingAttachmentJobs.remove(pendingAttachmentId)
        }?.cancel()

        conversationDraftDelegate.removePendingAttachment(
            pendingAttachmentId = pendingAttachmentId,
        )
    }

    override fun onRemoveResolvedAttachment(contentUri: String) {
        conversationDraftDelegate.removeAttachment(contentUri = contentUri)

        deleteTemporaryAttachment(contentUri = contentUri)

        synchronized(photoPickerAttachmentLock) {
            unregisterPhotoPickerAttachmentByAttachmentUri(
                attachmentContentUri = contentUri,
            )?.also { photoPickerContentUri ->
                photoPickerContentUris.remove(photoPickerContentUri)
            }
        }
    }

    override fun onScreenCleared() {
        cancelPendingAttachmentJobs()
    }

    private fun cancelPendingAttachmentJobs() {
        val jobs = synchronized(photoPickerAttachmentLock) {
            val jobs = pendingAttachmentJobs.values.toList()
            pendingAttachmentJobs.clear()
            photoPickerContentUris.clear()
            attachmentContentUriByPhotoPickerContentUri.clear()
            photoPickerContentUriByAttachmentContentUri.clear()
            publishPhotoPickerSourceContentUrisLocked()

            jobs
        }

        jobs.forEach { it.cancel() }
    }

    private fun registerPhotoPickerAttachment(photoPickerAttachment: PhotoPickerDraftAttachment) {
        val sourceContentUri = photoPickerAttachment.sourceContentUri
        val attachmentContentUri = photoPickerAttachment.draftAttachment.contentUri

        attachmentContentUriByPhotoPickerContentUri[sourceContentUri] = attachmentContentUri
        photoPickerContentUriByAttachmentContentUri[attachmentContentUri] = sourceContentUri
        publishPhotoPickerSourceContentUrisLocked()
    }

    private fun unregisterPhotoPickerAttachmentByPickerUri(
        photoPickerContentUri: String,
    ): String? {
        val attachmentContentUri = attachmentContentUriByPhotoPickerContentUri
            .remove(photoPickerContentUri)
            ?: return null

        photoPickerContentUriByAttachmentContentUri.remove(attachmentContentUri)
        publishPhotoPickerSourceContentUrisLocked()

        return attachmentContentUri
    }

    private fun unregisterPhotoPickerAttachmentByAttachmentUri(
        attachmentContentUri: String,
    ): String? {
        val photoPickerContentUri = photoPickerContentUriByAttachmentContentUri
            .remove(attachmentContentUri)
            ?: return null

        attachmentContentUriByPhotoPickerContentUri.remove(photoPickerContentUri)
        publishPhotoPickerSourceContentUrisLocked()

        return photoPickerContentUri
    }

    private fun publishPhotoPickerSourceContentUrisLocked() {
        _photoPickerSourceContentUriByAttachmentContentUri.value =
            photoPickerContentUriByAttachmentContentUri.toPersistentMap()
    }

    private fun deleteTemporaryAttachment(contentUri: String) {
        boundScope?.launch(defaultDispatcher) {
            conversationAttachmentRepository
                .deleteTemporaryAttachment(contentUri = contentUri)
                .collect()
        }
    }

    private companion object {
        private const val TAG = "ConversationMediaPickerDelegate"
    }
}
