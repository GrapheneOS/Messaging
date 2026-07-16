package com.android.messaging.ui.conversation.recipientpicker.model.picker

import com.android.messaging.data.conversation.model.ConversationId

internal sealed interface ConversationResolutionOutcome {
    data class Resolved(
        val conversationId: ConversationId,
    ) : ConversationResolutionOutcome

    data object Failed : ConversationResolutionOutcome
}
