package com.android.messaging.ui.conversation.messages.delegate

import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.repository.ConversationVCardMetadataRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.photoviewer.model.ConversationPhotoViewerAttachment
import com.android.messaging.domain.photoviewer.usecase.ResolveConversationPhotoViewerInitialOccurrenceIndex
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapper
import com.android.messaging.ui.conversation.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.messages.mapper.ConversationMessageUiModelMapper
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.util.ContentType
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal interface ConversationMessagesDelegate :
    ConversationScreenDelegate<ConversationMessagesUiState> {
    fun refresh()

    fun resolvePhotoViewerInitialOccurrenceIndex(
        contentType: String,
        partId: String,
        contentUri: String,
    ): Int
}

internal class ConversationMessagesDelegateImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val resolveInitialPhotoOccurrenceIndex:
    ResolveConversationPhotoViewerInitialOccurrenceIndex,
    private val conversationMessageUiModelMapper: ConversationMessageUiModelMapper,
    private val conversationVCardAttachmentUiModelMapper: ConversationVCardAttachmentUiModelMapper,
    private val conversationVCardMetadataRepository: ConversationVCardMetadataRepository,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationMessagesDelegate {

    private val _state = MutableStateFlow<ConversationMessagesUiState>(
        value = ConversationMessagesUiState.Loading,
    )
    private val currentMessages = MutableStateFlow<List<ConversationMessageData>>(
        value = emptyList(),
    )

    override val state = _state.asStateFlow()

    private val refreshTriggers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var isBound = false

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        if (isBound) {
            return
        }

        isBound = true

        scope.launch(defaultDispatcher) {
            conversationIdFlow.collectLatest { conversationId ->
                currentMessages.value = emptyList()
                _state.value = ConversationMessagesUiState.Loading

                if (conversationId == null) {
                    return@collectLatest
                }

                observeConversationMessagesUiState(
                    conversationId = conversationId,
                ).collect { currentMessagesUiState ->
                    _state.value = currentMessagesUiState
                }
            }
        }
    }

    override fun refresh() {
        refreshTriggers.tryEmit(Unit)
    }

    override fun resolvePhotoViewerInitialOccurrenceIndex(
        contentType: String,
        partId: String,
        contentUri: String,
    ): Int {
        return when {
            ContentType.isImageType(contentType) -> {
                resolveInitialPhotoOccurrenceIndex(
                    partId = partId,
                    contentUri = contentUri.toUri(),
                    attachments = buildConversationPhotoViewerAttachments(
                        messages = currentMessages.value,
                    ),
                )
            }

            else -> 0
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeConversationMessagesUiState(
        conversationId: String,
    ): Flow<ConversationMessagesUiState> {
        return conversationsRepository
            .getConversationMessages(conversationId = conversationId)
            .onEach { messages ->
                currentMessages.value = messages
            }
            .map { messages ->
                messages
                    .asSequence()
                    .map(conversationMessageUiModelMapper::map)
                    .toImmutableList()
            }
            .flatMapLatest { messages ->
                observeConversationMessagesUiState(
                    messages = messages,
                )
            }
            .flowOn(defaultDispatcher)
    }

    private fun observeConversationMessagesUiState(
        messages: List<ConversationMessageUiModel>,
    ): Flow<ConversationMessagesUiState> {
        val vCardContentUris = messages
            .asSequence()
            .flatMap { message -> message.parts.asSequence() }
            .mapNotNull { part ->
                (part as? ConversationMessagePartUiModel.Attachment.VCard)
                    ?.contentUri
                    ?.toString()
            }
            .distinct()
            .toList()

        if (vCardContentUris.isEmpty()) {
            return flowOf(
                ConversationMessagesUiState.Present(
                    messages = messages.toImmutableList(),
                ),
            )
        }

        val vCardMetadataFlows = vCardContentUris.map { contentUri ->
            conversationVCardMetadataRepository
                .observeAttachmentMetadata(
                    contentUri = contentUri,
                    refreshes = refreshTriggers,
                )
                .map { metadata ->
                    contentUri to metadata
                }
        }

        return combine(flows = vCardMetadataFlows) { contentUriAndMetadata ->
            val vCardAttachmentMetadata = contentUriAndMetadata.associate { pair ->
                pair.first to pair.second
            }

            ConversationMessagesUiState.Present(
                messages = updateMessagesWithVCardUiModel(
                    messages = messages,
                    vCardAttachmentMetadata = vCardAttachmentMetadata,
                ),
            )
        }
    }

    private fun updateMessagesWithVCardUiModel(
        messages: List<ConversationMessageUiModel>,
        vCardAttachmentMetadata: Map<String, ConversationVCardAttachmentMetadata>,
    ): ImmutableList<ConversationMessageUiModel> {
        return messages
            .map { message ->
                updateMessageUiModelWithVCardUiModel(
                    message = message,
                    vCardAttachmentMetadata = vCardAttachmentMetadata,
                )
            }
            .toImmutableList()
    }

    private fun updateMessageUiModelWithVCardUiModel(
        message: ConversationMessageUiModel,
        vCardAttachmentMetadata: Map<String, ConversationVCardAttachmentMetadata>,
    ): ConversationMessageUiModel {
        return message.copy(
            parts = message
                .parts
                .asSequence()
                .map { part ->
                    updateMessagePartUiModelWithVCardUiModel(
                        part = part,
                        vCardAttachmentMetadata = vCardAttachmentMetadata,
                    )
                }
                .toImmutableList(),
        )
    }

    private fun updateMessagePartUiModelWithVCardUiModel(
        part: ConversationMessagePartUiModel,
        vCardAttachmentMetadata: Map<String, ConversationVCardAttachmentMetadata>,
    ): ConversationMessagePartUiModel {
        return when (part) {
            is ConversationMessagePartUiModel.Attachment.VCard -> {
                val contentUri = part.contentUri?.toString()
                val metadata = contentUri?.let(vCardAttachmentMetadata::get)

                part.copy(
                    vCardUiModel = conversationVCardAttachmentUiModelMapper.map(
                        metadata = metadata,
                    ),
                )
            }

            is ConversationMessagePartUiModel.Attachment.Audio,
            is ConversationMessagePartUiModel.Attachment.File,
            is ConversationMessagePartUiModel.Attachment.Image,
            is ConversationMessagePartUiModel.Attachment.Video,
            is ConversationMessagePartUiModel.Text,
            -> {
                part
            }
        }
    }

    private fun buildConversationPhotoViewerAttachments(
        messages: List<ConversationMessageData>,
    ): Sequence<ConversationPhotoViewerAttachment> {
        return messages.asSequence().flatMap { message ->
            buildConversationPhotoViewerAttachments(message = message)
        }
    }

    private fun buildConversationPhotoViewerAttachments(
        message: ConversationMessageData,
    ): Sequence<ConversationPhotoViewerAttachment> {
        val parts = message.parts ?: return emptySequence()

        return parts
            .asSequence()
            .withIndex()
            .filter { indexedPart ->
                indexedPart.value.isImage
            }
            .sortedWith(comparator = photoViewerAttachmentPartComparator)
            .mapNotNull { indexedPart ->
                val part = indexedPart.value
                val contentUri = part.contentUri ?: return@mapNotNull null

                ConversationPhotoViewerAttachment(
                    partId = part.partId.orEmpty(),
                    contentUri = contentUri,
                )
            }
    }

    private companion object {
        private val photoViewerAttachmentPartComparator =
            compareBy<IndexedValue<MessagePartData>> { indexedPart ->
                indexedPart.value.partId?.toLongOrNull() == null
            }.thenBy { indexedPart ->
                indexedPart.value.partId?.toLongOrNull() ?: Long.MAX_VALUE
            }.thenBy { indexedPart ->
                indexedPart.value.partId.orEmpty()
            }.thenBy { indexedPart ->
                indexedPart.index
            }
    }
}
