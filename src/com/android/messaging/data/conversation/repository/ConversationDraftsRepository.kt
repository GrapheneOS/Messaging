package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapper
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.LogUtil
import javax.inject.Inject
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
    private val conversationDraftStore: ConversationDraftStore,
    private val conversationMetadataNotifier: ConversationMetadataNotifier,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationDraftsRepository {

    override fun observeConversationDraft(conversationId: String): Flow<ConversationDraft> {
        val draftChangeUri = MessagingContentProvider.buildConversationMetadataUri(conversationId)

        return observeDraftChanges(uri = draftChangeUri)
            .conflate()
            .map { loadConversationDraft(conversationId = conversationId) }
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
        withContext(context = ioDispatcher) {
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

            conversationMetadataNotifier.notifyConversationMetadataChanged(
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

    private fun loadConversationDraft(conversationId: String): ConversationDraft {
        val conversation = conversationDraftStore.getConversation(
            conversationId = conversationId,
        ) ?: return ConversationDraft()

        val draftMessage = conversationDraftStore.readDraftMessage(
            conversationId = conversationId,
            selfParticipantId = conversation.selfParticipantId,
        )

        return createConversationDraft(
            conversation = conversation,
            draftMessage = draftMessage,
        )
    }

    private fun createConversationDraft(
        conversation: ConversationDraftConversation,
        draftMessage: MessageData?,
    ): ConversationDraft {
        val attachments = draftMessage
            ?.parts
            ?.asSequence()
            ?.filter { part -> part.isAttachment }
            ?.mapNotNull(::createDraftAttachmentOrNull)
            ?.toList()
            ?: emptyList()

        val selfParticipantId = draftMessage
            ?.selfId
            ?.takeIf { selfParticipantId -> selfParticipantId.isNotBlank() }
            ?: conversation.selfParticipantId

        return ConversationDraft(
            messageText = draftMessage?.messageText.orEmpty(),
            subjectText = draftMessage?.mmsSubject.orEmpty(),
            selfParticipantId = selfParticipantId,
            attachments = attachments,
        )
    }

    private fun bindDraftParticipantsIfNeeded(
        conversationId: String,
        message: MessageData,
    ): MessageData? {
        if (message.selfId != null && message.participantId != null) {
            return message
        }

        val conversation = conversationDraftStore.getConversation(
            conversationId = conversationId,
        ) ?: run {
            LogUtil.w(
                TAG,
                "Conversation $conversationId was deleted before saving draft ${message.messageId}",
            )
            return null
        }

        val selfParticipantId = conversation.selfParticipantId
        if (message.selfId == null) {
            message.bindSelfId(selfParticipantId)
        }

        if (message.participantId == null) {
            message.bindParticipantId(selfParticipantId)
        }

        return message
    }

    private fun createDraftAttachmentOrNull(part: MessagePartData): ConversationDraftAttachment? {
        val contentType = part.contentType?.takeIf { it.isNotBlank() }
        val contentUri = part.contentUri?.toString()?.takeIf { it.isNotBlank() }

        return when {
            contentType != null && contentUri != null -> {
                ConversationDraftAttachment(
                    contentType = contentType,
                    contentUri = contentUri,
                    captionText = part.text.orEmpty(),
                    width = normalizePartDimension(size = part.width),
                    height = normalizePartDimension(size = part.height),
                )
            }

            else -> {
                LogUtil.w(TAG, "Dropping draft attachment with blank contentType or contentUri")

                null
            }
        }
    }

    private fun normalizePartDimension(size: Int): Int? {
        return size.takeIf { it != MessagePartData.UNSPECIFIED_SIZE }
    }

    private companion object {
        private const val TAG = "ConversationDraftsRepository"
    }
}
