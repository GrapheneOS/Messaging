package com.android.messaging.ui.conversation.composer.delegate

import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.data.conversation.repository.ConversationVCardMetadataRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapper
import com.android.messaging.ui.conversation.composer.mapper.ConversationComposerAttachmentUiModelMapper
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

internal interface ConversationComposerAttachmentsDelegate {
    val state: StateFlow<ImmutableList<ComposerAttachmentUiModel>>

    fun bind(
        scope: CoroutineScope,
        draftStateFlow: StateFlow<ConversationDraftState>,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationComposerAttachmentsDelegateImpl @Inject constructor(
    private val conversationComposerAttachmentUiModelMapper:
    ConversationComposerAttachmentUiModelMapper,
    private val conversationVCardAttachmentUiModelMapper: ConversationVCardAttachmentUiModelMapper,
    private val conversationVCardMetadataRepository: ConversationVCardMetadataRepository,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationComposerAttachmentsDelegate {

    private val _state = MutableStateFlow<ImmutableList<ComposerAttachmentUiModel>>(
        value = persistentListOf(),
    )

    override val state = _state.asStateFlow()

    private var isBound = false

    override fun bind(
        scope: CoroutineScope,
        draftStateFlow: StateFlow<ConversationDraftState>,
    ) {
        if (isBound) {
            return
        }

        isBound = true
        _state.value = mapAttachmentUiModels(
            attachmentSource = createAttachmentSource(
                draftState = draftStateFlow.value,
            ),
        )

        scope.launch(defaultDispatcher) {
            draftStateFlow
                .map(::createAttachmentSource)
                .distinctUntilChanged()
                .flatMapLatest(::observeAttachmentUiModels)
                .collect { attachmentUiModels ->
                    _state.value = attachmentUiModels
                }
        }
    }

    private fun createAttachmentSource(
        draftState: ConversationDraftState,
    ): ComposerAttachmentSource {
        return ComposerAttachmentSource(
            attachments = draftState.draft.attachments,
            pendingAttachments = draftState.pendingAttachments,
        )
    }

    private fun observeAttachmentUiModels(
        attachmentSource: ComposerAttachmentSource,
    ): Flow<ImmutableList<ComposerAttachmentUiModel>> {
        val attachmentUiModels = mapAttachmentUiModels(
            attachmentSource = attachmentSource,
        )
        val vCardContentUris = attachmentUiModels
            .asSequence()
            .filterIsInstance<ComposerAttachmentUiModel.Resolved.VCard>()
            .map { it.contentUri }
            .distinct()
            .toList()

        if (vCardContentUris.isEmpty()) {
            return flowOf(attachmentUiModels)
        }

        val metadataFlows = vCardContentUris.map { contentUri ->
            conversationVCardMetadataRepository
                .observeAttachmentMetadata(contentUri = contentUri)
                .map { metadata ->
                    contentUri to metadata
                }
        }

        return combine(flows = metadataFlows) { contentUriAndMetadata ->
            val vCardAttachmentMetadata = contentUriAndMetadata.associate { pair ->
                pair.first to pair.second
            }

            updateAttachmentUiModelsWithVCardUiModel(
                attachments = attachmentUiModels,
                vCardAttachmentMetadata = vCardAttachmentMetadata,
            )
        }
            .onStart {
                emit(attachmentUiModels)
            }
            .flowOn(defaultDispatcher)
    }

    private fun mapAttachmentUiModels(
        attachmentSource: ComposerAttachmentSource,
    ): ImmutableList<ComposerAttachmentUiModel> {
        return conversationComposerAttachmentUiModelMapper.map(
            attachments = attachmentSource.attachments,
            pendingAttachments = attachmentSource.pendingAttachments,
        )
    }

    private fun updateAttachmentUiModelsWithVCardUiModel(
        attachments: ImmutableList<ComposerAttachmentUiModel>,
        vCardAttachmentMetadata: Map<String, ConversationVCardAttachmentMetadata>,
    ): ImmutableList<ComposerAttachmentUiModel> {
        return attachments
            .map { attachment ->
                updateAttachmentUiModelWithVCardUiModel(
                    attachment = attachment,
                    vCardAttachmentMetadata = vCardAttachmentMetadata,
                )
            }
            .toImmutableList()
    }

    private fun updateAttachmentUiModelWithVCardUiModel(
        attachment: ComposerAttachmentUiModel,
        vCardAttachmentMetadata: Map<String, ConversationVCardAttachmentMetadata>,
    ): ComposerAttachmentUiModel {
        return when (attachment) {
            is ComposerAttachmentUiModel.Pending.AudioFinalizing,
            is ComposerAttachmentUiModel.Pending.Generic,
            -> {
                attachment
            }

            is ComposerAttachmentUiModel.Resolved.Audio,
            is ComposerAttachmentUiModel.Resolved.File,
            is ComposerAttachmentUiModel.Resolved.VisualMedia.Image,
            is ComposerAttachmentUiModel.Resolved.VisualMedia.Video,
            -> {
                attachment
            }

            is ComposerAttachmentUiModel.Resolved.VCard -> {
                val resolvedVCardMetadata = vCardAttachmentMetadata[attachment.contentUri]
                    ?: ConversationVCardAttachmentMetadata.Loading

                attachment.copy(
                    vCardUiModel = conversationVCardAttachmentUiModelMapper.map(
                        metadata = resolvedVCardMetadata,
                    ),
                )
            }
        }
    }

    private data class ComposerAttachmentSource(
        val attachments: List<ConversationDraftAttachment>,
        val pendingAttachments: List<ConversationDraftPendingAttachment>,
    )
}
