package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapper
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapper
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.store.ConversationDraftStore
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.di.core.MessagingDbDispatcher
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import com.android.messaging.util.MediaMetadataRetrieverWrapper
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal interface ConversationDraftsRepository {
    fun observeConversationDraft(conversationId: String): Flow<ConversationDraft>

    suspend fun saveDraft(
        conversationId: String,
        draft: ConversationDraft,
    )
}

internal class ConversationDraftsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val conversationDraftMessageDataMapper: ConversationDraftMessageDataMapper,
    private val conversationMessageDataDraftMapper: ConversationMessageDataDraftMapper,
    private val conversationDraftStore: ConversationDraftStore,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
    @param:MessagingDbDispatcher
    private val messagingDbDispatcher: CoroutineDispatcher,
) : ConversationDraftsRepository {

    override fun observeConversationDraft(conversationId: String): Flow<ConversationDraft> {
        val draftChangeUri = MessagingContentProvider.buildConversationMetadataUri(conversationId)

        return observeDraftChanges(uri = draftChangeUri)
            .flowOn(defaultDispatcher)
            .conflate()
            .map { loadConversationDraft(conversationId) }
            .flowOn(messagingDbDispatcher)
            .map(::resolveDraftAttachmentMetadata)
            .catch { e ->
                LogUtil.e(
                    TAG,
                    "Failed to load draft for conversation $conversationId",
                    e,
                )

                emit(ConversationDraft())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveDraft(
        conversationId: String,
        draft: ConversationDraft,
    ) {
        withContext(context = messagingDbDispatcher) {
            val message = conversationDraftMessageDataMapper.map(
                conversationId = conversationId,
                draft = draft,
            )
            val boundMessage = bindDraftParticipantsIfNeeded(
                conversationId = conversationId,
                message = message,
            ) ?: return@withContext

            conversationDraftStore.updateDraftMessage(
                conversationId = conversationId,
                message = boundMessage,
            )

            notifyConversationMetadataChanged(
                conversationId = conversationId,
            )
        }
    }

    private fun observeDraftChanges(uri: Uri): Flow<Unit> {
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

    private fun notifyConversationMetadataChanged(conversationId: String) {
        MessagingContentProvider.notifyConversationMetadataChanged(conversationId)
    }

    private fun loadConversationDraft(conversationId: String): ConversationDraft {
        val selfParticipantId = conversationDraftStore.getSelfParticipantId(
            conversationId = conversationId,
        ) ?: return ConversationDraft()

        val draftMessage = conversationDraftStore.readDraftMessage(
            conversationId = conversationId,
            selfParticipantId = selfParticipantId,
        )

        return createConversationDraft(
            selfParticipantId = selfParticipantId,
            draftMessage = draftMessage,
        )
    }

    private fun createConversationDraft(
        selfParticipantId: String,
        draftMessage: MessageData?,
    ): ConversationDraft {
        return when (draftMessage) {
            null -> {
                ConversationDraft(
                    selfParticipantId = selfParticipantId,
                )
            }

            else -> {
                conversationMessageDataDraftMapper.map(
                    messageData = draftMessage,
                    fallbackSelfParticipantId = selfParticipantId,
                )
            }
        }
    }

    private fun resolveDraftAttachmentMetadata(draft: ConversationDraft): ConversationDraft {
        val hasAudioAttachments = draft.attachments.any { attachment ->
            ContentType.isAudioType(attachment.contentType)
        }

        return when {
            hasAudioAttachments -> resolveDraftAudioMetadata(draft = draft)
            else -> draft
        }
    }

    private fun resolveDraftAudioMetadata(draft: ConversationDraft): ConversationDraft {
        var hasChanges = false

        val resolvedAttachments = draft
            .attachments
            .map { attachment ->
                val isAudio = ContentType.isAudioType(attachment.contentType)

                if (!isAudio || attachment.durationMillis != null) {
                    return@map attachment
                }

                hasChanges = true
                attachment.copy(
                    durationMillis = resolveAudioDurationMillis(
                        contentUri = attachment.contentUri,
                    ),
                )
            }

        return when {
            hasChanges -> {
                draft.copy(
                    attachments = resolvedAttachments.toImmutableList(),
                )
            }
            else -> draft
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun resolveAudioDurationMillis(contentUri: String): Long {
        val mediaMetadataRetrieverWrapper = MediaMetadataRetrieverWrapper()

        return try {
            mediaMetadataRetrieverWrapper.setDataSource(contentUri.toUri())
            mediaMetadataRetrieverWrapper
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(minimumValue = 0L)
                ?: 0L
        } catch (exception: Exception) {
            LogUtil.w(
                TAG,
                "Failed to resolve draft audio duration for $contentUri",
                exception,
            )

            0L
        } finally {
            mediaMetadataRetrieverWrapper.release()
        }
    }

    private fun bindDraftParticipantsIfNeeded(
        conversationId: String,
        message: MessageData,
    ): MessageData? {
        if (hasDraftParticipants(message = message)) {
            return message
        }

        return conversationDraftStore
            .getSelfParticipantId(conversationId)
            ?.let { selfParticipantId ->
                bindMissingDraftParticipants(
                    message = message,
                    selfParticipantId = selfParticipantId,
                )
            }
    }

    private fun hasDraftParticipants(message: MessageData): Boolean {
        return message.selfId != null && message.participantId != null
    }

    private fun bindMissingDraftParticipants(
        message: MessageData,
        selfParticipantId: String,
    ): MessageData {
        if (selfParticipantId.isBlank()) {
            return message
        }

        if (message.selfId == null) {
            message.bindSelfId(selfParticipantId)
        }

        if (message.participantId == null) {
            message.bindParticipantId(selfParticipantId)
        }

        return message
    }

    private companion object {
        private const val TAG = "ConversationDraftsRepository"
    }
}
