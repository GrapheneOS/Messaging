package com.android.messaging.ui.conversation.composer.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.data.subscription.resolveSelectedSubscription
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
        defaultSmsSubscriptionId: Int,
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
        defaultSmsSubscriptionId: Int,
    ): ConversationComposerUiState {
        val draft = draftState.draft
        val hasWorkingDraft = draft.hasContent
        val visibleSendProtocol = getSendProtocol(draftState)

        val isComposerEditable = composerAvailability is ConversationComposerAvailability.Editable
        val isDraftInteractionAvailable = draft.isInteractionAvailable()
        val isComposerInteractionAvailable = isComposerEditable && isDraftInteractionAvailable
        val isPrimaryActionAvailable = isComposerInteractionAvailable &&
            draftState.pendingAttachments.isEmpty()

        val shouldShowRecordAction = !hasWorkingDraft &&
            audioRecording.phase == ConversationAudioRecordingPhase.Idle

        val isSendEnabled = hasWorkingDraft && isPrimaryActionAvailable

        val simSelector = buildSimSelectorUiState(
            subscriptions = subscriptions,
            selfParticipantId = draft.selfParticipantId,
            areSubscriptionsLoaded = areSubscriptionsLoaded,
            defaultSmsSubscriptionId = defaultSmsSubscriptionId,
        )

        return ConversationComposerUiState(
            audioRecording = audioRecording,
            attachments = attachments,
            messageText = draft.messageText,
            subjectText = draft.subjectText,
            selfParticipantId = draft.selfParticipantId,
            simSelector = simSelector,
            isMessageFieldEnabled = isComposerEditable,
            isAttachmentActionEnabled = isComposerInteractionAvailable,
            isRecordActionEnabled = isPrimaryActionAvailable,
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
            disabledReason = when (composerAvailability) {
                ConversationComposerAvailability.Editable -> null
                is ConversationComposerAvailability.Unavailable -> composerAvailability.reason
            },
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
        defaultSmsSubscriptionId: Int,
    ): ConversationSimSelectorUiState {
        val selected = resolveSelectedSubscription(
            subscriptions = subscriptions,
            selectedSelfParticipantId = selfParticipantId,
            defaultSmsSubscriptionId = defaultSmsSubscriptionId,
        )

        return ConversationSimSelectorUiState(
            subscriptions = subscriptions,
            selectedSubscription = selected,
            isLoading = !areSubscriptionsLoaded,
        )
    }

    private fun getSendProtocol(
        draftState: ConversationDraftState,
    ): ConversationDraftSendProtocol {
        return when {
            draftState.draft.hasContent -> draftState.sendProtocol
            else -> ConversationDraftSendProtocol.SMS
        }
    }

    private fun ConversationDraft.isInteractionAvailable(): Boolean {
        return !isCheckingDraft && !isSending
    }

    private companion object {
        private const val SEGMENT_COUNTER_VISIBILITY_THRESHOLD = 10
    }
}
