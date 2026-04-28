package com.android.messaging.ui.conversation.v2.composer.mapper

import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationSubscription
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.v2.composer.model.ConversationSimSelectorUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList

internal interface ConversationComposerUiStateMapper {
    fun map(
        audioRecording: ConversationAudioRecordingUiState,
        draftState: ConversationDraftState,
        attachments: ImmutableList<ComposerAttachmentUiModel>,
        composerAvailability: ConversationComposerAvailability,
        subscriptions: ImmutableList<ConversationSubscription>,
    ): ConversationComposerUiState
}

internal class ConversationComposerUiStateMapperImpl @Inject constructor() :
    ConversationComposerUiStateMapper {

    override fun map(
        audioRecording: ConversationAudioRecordingUiState,
        draftState: ConversationDraftState,
        attachments: ImmutableList<ComposerAttachmentUiModel>,
        composerAvailability: ConversationComposerAvailability,
        subscriptions: ImmutableList<ConversationSubscription>,
    ): ConversationComposerUiState {
        val draft = draftState.draft
        val hasWorkingDraft = draft.hasContent
        val visibleSendProtocol = when {
            hasWorkingDraft -> draftState.sendProtocol
            else -> ConversationDraftSendProtocol.SMS
        }

        val isAttachmentActionEnabled = composerAvailability.isAttachmentActionEnabled &&
            !draft.isCheckingDraft &&
            !draft.isSending

        val isMessageFieldEnabled = composerAvailability.isMessageFieldEnabled
        val shouldShowRecordAction = !hasWorkingDraft &&
            audioRecording.phase == ConversationAudioRecordingPhase.Idle

        val isRecordActionEnabled = composerAvailability.isSendAvailable &&
            !draft.isCheckingDraft &&
            !draft.isSending &&
            draftState.pendingAttachments.isEmpty()

        val isSendEnabled = composerAvailability.isSendAvailable &&
            hasWorkingDraft &&
            !draft.isCheckingDraft &&
            !draft.isSending &&
            draftState.pendingAttachments.isEmpty()

        return ConversationComposerUiState(
            audioRecording = audioRecording,
            attachments = attachments,
            messageText = draft.messageText,
            subjectText = draft.subjectText,
            selfParticipantId = draft.selfParticipantId,
            simSelector = buildSimSelectorUiState(
                subscriptions = subscriptions,
                selfParticipantId = draft.selfParticipantId,
            ),
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isRecordActionEnabled = isRecordActionEnabled,
            isSendEnabled = isSendEnabled,
            shouldShowRecordAction = shouldShowRecordAction,
            hasWorkingDraft = hasWorkingDraft,
            sendProtocol = visibleSendProtocol,
            attachmentCount = draft.attachments.size,
            pendingAttachmentCount = draftState.pendingAttachments.size,
            messageCount = draft.messageCount,
            codePointsRemainingInCurrentMessage = draft.codePointsRemainingInCurrentMessage,
            isCheckingDraft = draft.isCheckingDraft,
            isSending = draft.isSending,
            disabledReason = composerAvailability.disabledReason,
        )
    }

    private fun buildSimSelectorUiState(
        subscriptions: ImmutableList<ConversationSubscription>,
        selfParticipantId: String,
    ): ConversationSimSelectorUiState {
        val selected = subscriptions
            .firstOrNull { it.selfParticipantId == selfParticipantId }
            ?: subscriptions.firstOrNull()

        return ConversationSimSelectorUiState(
            subscriptions = subscriptions,
            selectedSubscription = selected,
        )
    }
}
