package com.android.messaging.ui.conversation.composer.model

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol

internal data class ConversationDraftState(
    val draft: ConversationDraft = ConversationDraft(),
    val pendingAttachments: List<ConversationDraftPendingAttachment> = emptyList(),
    val sendProtocol: ConversationDraftSendProtocol = ConversationDraftSendProtocol.SMS,
)
