package com.android.messaging.ui.conversation.v2.composer.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerUiState
import javax.inject.Inject

internal interface ConversationComposerUiStateMapper {
    fun map(
        draft: ConversationDraft,
        composerAvailability: ConversationComposerAvailability,
    ): ConversationComposerUiState
}

internal class ConversationComposerUiStateMapperImpl @Inject constructor() :
    ConversationComposerUiStateMapper {

    override fun map(
        draft: ConversationDraft,
        composerAvailability: ConversationComposerAvailability,
    ): ConversationComposerUiState {
        val hasWorkingDraft = draft.hasContent

        val isSendEnabled = composerAvailability.isSendAvailable &&
            hasWorkingDraft &&
            !draft.isCheckingDraft &&
            !draft.isSending

        return ConversationComposerUiState(
            messageText = draft.messageText,
            subjectText = draft.subjectText,
            selfParticipantId = draft.selfParticipantId,
            isMessageFieldEnabled = composerAvailability.isMessageFieldEnabled,
            isAttachmentActionEnabled = composerAvailability.isAttachmentActionEnabled,
            isSendEnabled = isSendEnabled,
            hasWorkingDraft = hasWorkingDraft,
            isMms = draft.isMms,
            attachmentCount = draft.attachments.size,
            pendingAttachmentCount = 0,
            messageCount = draft.messageCount,
            codePointsRemainingInCurrentMessage = draft.codePointsRemainingInCurrentMessage,
            isCheckingDraft = draft.isCheckingDraft,
            isSending = draft.isSending,
            disabledReason = composerAvailability.disabledReason,
        )
    }
}
