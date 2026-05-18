package com.android.messaging.ui.conversation.composer.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.MessageTextStats
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.composer.model.ConversationSegmentCounterUiState
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList

internal interface ConversationComposerUiStateMapper {
    fun map(
        audioRecording: ConversationAudioRecordingUiState,
        draftState: ConversationDraftState,
        attachments: ImmutableList<ComposerAttachmentUiModel>,
        composerAvailability: ConversationComposerAvailability,
        subscriptions: ImmutableList<Subscription>,
        areSubscriptionsLoaded: Boolean,
    ): ConversationComposerUiState
}

internal class ConversationComposerUiStateMapperImpl @Inject constructor() :
    ConversationComposerUiStateMapper {

    override fun map(
        audioRecording: ConversationAudioRecordingUiState,
        draftState: ConversationDraftState,
        attachments: ImmutableList<ComposerAttachmentUiModel>,
        composerAvailability: ConversationComposerAvailability,
        subscriptions: ImmutableList<Subscription>,
        areSubscriptionsLoaded: Boolean,
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

        val simSelector = buildSimSelectorUiState(
            subscriptions = subscriptions,
            selfParticipantId = draft.selfParticipantId,
            areSubscriptionsLoaded = areSubscriptionsLoaded,
        )

        return ConversationComposerUiState(
            audioRecording = audioRecording,
            attachments = attachments,
            messageText = draft.messageText,
            subjectText = draft.subjectText,
            selfParticipantId = draft.selfParticipantId,
            simSelector = simSelector,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isRecordActionEnabled = isRecordActionEnabled,
            isSendEnabled = isSendEnabled,
            shouldShowRecordAction = shouldShowRecordAction,
            hasWorkingDraft = hasWorkingDraft,
            sendProtocol = visibleSendProtocol,
            attachmentCount = draft.attachments.size,
            pendingAttachmentCount = draftState.pendingAttachments.size,
            segmentCounter = buildSegmentCounterUiState(
                draft = draft,
                sendProtocol = visibleSendProtocol,
                selfSubId = simSelector.selectedSubscription?.subId
                    ?: ParticipantData.DEFAULT_SELF_SUB_ID,
            ),
            isCheckingDraft = draft.isCheckingDraft,
            isSending = draft.isSending,
            disabledReason = composerAvailability.disabledReason,
        )
    }

    private fun buildSegmentCounterUiState(
        draft: ConversationDraft,
        sendProtocol: ConversationDraftSendProtocol,
        selfSubId: Int,
    ): ConversationSegmentCounterUiState? {
        val isSms = sendProtocol == ConversationDraftSendProtocol.SMS
        val messageText = draft.messageText

        if (!isSms || messageText.isBlank()) {
            return null
        }

        val stats = MessageTextStats().apply {
            updateMessageTextStats(selfSubId, messageText)
        }

        val messageCount = stats.numMessagesToBeSent
        val codePointsRemaining = stats.codePointsRemainingInCurrentMessage

        val isVisible = messageCount > 1 ||
            codePointsRemaining <= SEGMENT_COUNTER_VISIBILITY_THRESHOLD

        return when {
            isVisible -> {
                ConversationSegmentCounterUiState(
                    codePointsRemainingInCurrentMessage = codePointsRemaining,
                    messageCount = messageCount,
                )
            }

            else -> null
        }
    }

    private fun buildSimSelectorUiState(
        subscriptions: ImmutableList<Subscription>,
        selfParticipantId: String,
        areSubscriptionsLoaded: Boolean,
    ): ConversationSimSelectorUiState {
        val selected = subscriptions
            .firstOrNull { it.selfParticipantId == selfParticipantId }
            ?: subscriptions.firstOrNull()

        return ConversationSimSelectorUiState(
            subscriptions = subscriptions,
            selectedSubscription = selected,
            isLoading = !areSubscriptionsLoaded,
        )
    }

    private companion object {
        private const val SEGMENT_COUNTER_VISIBILITY_THRESHOLD = 10
    }
}
