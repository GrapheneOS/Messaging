package com.android.messaging.ui.conversation.v2.composer.mapper

import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerAttachmentUiState
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationComposerUiStateMapper {
    fun map(
        draftState: ConversationDraftState,
        composerAvailability: ConversationComposerAvailability,
    ): ConversationComposerUiState
}

internal class ConversationComposerUiStateMapperImpl @Inject constructor() :
    ConversationComposerUiStateMapper {

    override fun map(
        draftState: ConversationDraftState,
        composerAvailability: ConversationComposerAvailability,
    ): ConversationComposerUiState {
        val draft = draftState.draft
        val hasWorkingDraft = draft.hasContent

        val isAttachmentActionEnabled = composerAvailability.isAttachmentActionEnabled &&
            !draft.isCheckingDraft &&
            !draft.isSending

        val isMessageFieldEnabled = composerAvailability.isMessageFieldEnabled

        val isSendEnabled = composerAvailability.isSendAvailable &&
            hasWorkingDraft &&
            !draft.isCheckingDraft &&
            !draft.isSending &&
            draftState.pendingAttachments.isEmpty()

        return ConversationComposerUiState(
            attachments = draftState.toAttachmentUiState(),
            messageText = draft.messageText,
            subjectText = draft.subjectText,
            selfParticipantId = draft.selfParticipantId,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isSendEnabled = isSendEnabled,
            hasWorkingDraft = hasWorkingDraft,
            isMms = draft.isMms,
            attachmentCount = draft.attachments.size,
            pendingAttachmentCount = draftState.pendingAttachments.size,
            messageCount = draft.messageCount,
            codePointsRemainingInCurrentMessage = draft.codePointsRemainingInCurrentMessage,
            isCheckingDraft = draft.isCheckingDraft,
            isSending = draft.isSending,
            disabledReason = composerAvailability.disabledReason,
        )
    }

    private fun ConversationDraftState.toAttachmentUiState():
        ImmutableList<ConversationComposerAttachmentUiState> {
        val resolvedAttachments = draft.attachments.map { attachment ->
            ConversationComposerAttachmentUiState.Resolved(
                key = attachment.contentUri,
                contentType = attachment.contentType,
                contentUri = attachment.contentUri,
                captionText = attachment.captionText,
                width = attachment.width,
                height = attachment.height,
            )
        }

        val pendingAttachments = pendingAttachments.map { pendingAttachment ->
            ConversationComposerAttachmentUiState.Pending(
                key = pendingAttachment.pendingAttachmentId,
                contentType = pendingAttachment.contentType,
                contentUri = pendingAttachment.contentUri,
                displayName = pendingAttachment.displayName,
            )
        }

        return (resolvedAttachments + pendingAttachments).toImmutableList()
    }
}
