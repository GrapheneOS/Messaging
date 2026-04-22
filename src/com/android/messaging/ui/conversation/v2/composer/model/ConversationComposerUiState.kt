package com.android.messaging.ui.conversation.v2.composer.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.metadata.ConversationComposerDisabledReason
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ConversationComposerUiState(
    val audioRecording: ConversationAudioRecordingUiState = ConversationAudioRecordingUiState(),
    val attachments: ImmutableList<ComposerAttachmentUiModel> = persistentListOf(),
    val messageText: String = "",
    val subjectText: String = "",
    val selfParticipantId: String = "",
    val simSelector: ConversationSimSelectorUiState = ConversationSimSelectorUiState(),
    val isMessageFieldEnabled: Boolean = false,
    val isAttachmentActionEnabled: Boolean = false,
    val isRecordActionEnabled: Boolean = false,
    val isSendEnabled: Boolean = false,
    val shouldShowRecordAction: Boolean = false,
    val hasWorkingDraft: Boolean = false,
    val isMms: Boolean = false,
    val attachmentCount: Int = 0,
    val pendingAttachmentCount: Int = 0,
    val messageCount: Int = 1,
    val codePointsRemainingInCurrentMessage: Int = 0,
    val isCheckingDraft: Boolean = false,
    val isSending: Boolean = false,
    val disabledReason: ConversationComposerDisabledReason? = null,
)
