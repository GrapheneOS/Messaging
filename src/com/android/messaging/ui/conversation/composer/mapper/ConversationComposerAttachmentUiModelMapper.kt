package com.android.messaging.ui.conversation.composer.mapper

import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachmentKind
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapper
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.util.ContentType
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationComposerAttachmentUiModelMapper {
    fun map(
        attachments: List<ConversationDraftAttachment>,
        pendingAttachments: List<ConversationDraftPendingAttachment>,
    ): ImmutableList<ComposerAttachmentUiModel>
}

internal class ConversationComposerAttachmentUiModelMapperImpl @Inject constructor(
    private val conversationVCardAttachmentUiModelMapper: ConversationVCardAttachmentUiModelMapper,
) : ConversationComposerAttachmentUiModelMapper {

    override fun map(
        attachments: List<ConversationDraftAttachment>,
        pendingAttachments: List<ConversationDraftPendingAttachment>,
    ): ImmutableList<ComposerAttachmentUiModel> {
        val resolvedAttachments = attachments.map { attachment ->
            createResolvedAttachmentUiModel(
                attachment = attachment,
            )
        }
        val pendingAttachmentUiModels = pendingAttachments.map { pendingAttachment ->
            createPendingAttachmentUiModel(
                pendingAttachment = pendingAttachment,
            )
        }

        return (resolvedAttachments + pendingAttachmentUiModels).toImmutableList()
    }

    private fun createPendingAttachmentUiModel(
        pendingAttachment: ConversationDraftPendingAttachment,
    ): ComposerAttachmentUiModel.Pending {
        return when (pendingAttachment.kind) {
            ConversationDraftPendingAttachmentKind.Generic -> {
                ComposerAttachmentUiModel.Pending.Generic(
                    key = pendingAttachment.pendingAttachmentId,
                    contentType = pendingAttachment.contentType,
                    contentUri = pendingAttachment.contentUri,
                    displayName = pendingAttachment.displayName,
                )
            }

            ConversationDraftPendingAttachmentKind.AudioFinalizing -> {
                ComposerAttachmentUiModel.Pending.AudioFinalizing(
                    key = pendingAttachment.pendingAttachmentId,
                    contentType = pendingAttachment.contentType,
                    contentUri = pendingAttachment.contentUri,
                    displayName = pendingAttachment.displayName,
                )
            }
        }
    }

    private fun createResolvedAttachmentUiModel(
        attachment: ConversationDraftAttachment,
    ): ComposerAttachmentUiModel.Resolved {
        return when {
            ContentType.isAudioType(attachment.contentType) -> {
                ComposerAttachmentUiModel.Resolved.Audio(
                    key = attachment.contentUri,
                    contentType = attachment.contentType,
                    contentUri = attachment.contentUri,
                    durationMillis = attachment.durationMillis ?: 0L,
                )
            }

            ContentType.isImageType(attachment.contentType) -> {
                ComposerAttachmentUiModel.Resolved.VisualMedia.Image(
                    key = attachment.contentUri,
                    contentType = attachment.contentType,
                    contentUri = attachment.contentUri,
                    captionText = attachment.captionText,
                    width = attachment.width,
                    height = attachment.height,
                )
            }

            ContentType.isVCardType(attachment.contentType) -> {
                ComposerAttachmentUiModel.Resolved.VCard(
                    key = attachment.contentUri,
                    contentType = attachment.contentType,
                    contentUri = attachment.contentUri,
                    vCardUiModel = conversationVCardAttachmentUiModelMapper.map(
                        metadata = ConversationVCardAttachmentMetadata.Loading,
                    ),
                )
            }

            ContentType.isVideoType(attachment.contentType) -> {
                ComposerAttachmentUiModel.Resolved.VisualMedia.Video(
                    key = attachment.contentUri,
                    contentType = attachment.contentType,
                    contentUri = attachment.contentUri,
                    captionText = attachment.captionText,
                    width = attachment.width,
                    height = attachment.height,
                )
            }

            else -> {
                ComposerAttachmentUiModel.Resolved.File(
                    key = attachment.contentUri,
                    contentType = attachment.contentType,
                    contentUri = attachment.contentUri,
                )
            }
        }
    }
}
