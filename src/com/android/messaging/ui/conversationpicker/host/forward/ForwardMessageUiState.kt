package com.android.messaging.ui.conversationpicker.host.forward

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.data.MessageData

internal data class ForwardMessageUiState(
    val draft: ConversationDraft? = null,
    val message: MessageData? = null,
    val isLoading: Boolean = true,
)
