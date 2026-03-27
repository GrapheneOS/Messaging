package com.android.messaging.ui.conversation.v2.composer.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.metadata.ConversationComposerDisabledReason

@Immutable
internal data class ConversationComposerUiState(
    val messageText: String = "",
    val subjectText: String = "",
    val selfParticipantId: String = "",
    val isMessageFieldEnabled: Boolean = false,
    val isAttachmentActionEnabled: Boolean = false,
    val isSendEnabled: Boolean = false,
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
