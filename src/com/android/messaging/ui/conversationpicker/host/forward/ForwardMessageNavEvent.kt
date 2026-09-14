package com.android.messaging.ui.conversationpicker.host.forward

import com.android.messaging.data.conversation.model.ConversationId

internal sealed interface ForwardMessageNavEvent {

    data class OpenConversation(
        val conversationId: ConversationId,
    ) : ForwardMessageNavEvent

    data object Close : ForwardMessageNavEvent
}
