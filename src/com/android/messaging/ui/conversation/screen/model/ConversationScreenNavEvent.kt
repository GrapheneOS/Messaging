package com.android.messaging.ui.conversation.screen.model

import com.android.messaging.data.conversation.model.MessageId

internal sealed interface ConversationScreenNavEvent {

    data object CloseConversation : ConversationScreenNavEvent

    data class NavigateToMessageDetails(
        val messageId: MessageId,
    ) : ConversationScreenNavEvent

    data class ForwardMessage(
        val messageId: MessageId,
    ) : ConversationScreenNavEvent
}
