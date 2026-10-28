package com.android.messaging.ui.conversationlist.chats.model

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId

internal sealed interface ConversationListNavEvent {

    data object OpenNewChat : ConversationListNavEvent
    data object OpenArchivedConversations : ConversationListNavEvent
    data object OpenBlockedParticipants : ConversationListNavEvent
    data object OpenSettings : ConversationListNavEvent

    data class OpenConversation(
        val conversationId: ConversationId,
    ) : ConversationListNavEvent

    data class OpenMessage(
        val conversationId: ConversationId,
        val messageId: MessageId,
    ) : ConversationListNavEvent

    data class OpenConversationSettings(
        val conversationId: ConversationId,
    ) : ConversationListNavEvent

    data class CloseConversation(
        val conversationId: ConversationId,
    ) : ConversationListNavEvent
}
