package com.android.messaging.ui.conversationlist.chats.model

import com.android.messaging.data.conversation.model.ConversationId

internal sealed interface ConversationListNavEvent {

    data object OpenNewChat : ConversationListNavEvent
    data object OpenArchivedConversations : ConversationListNavEvent
    data object OpenBlockedParticipants : ConversationListNavEvent
    data object OpenSettings : ConversationListNavEvent

    data class OpenConversation(
        val conversationId: ConversationId,
    ) : ConversationListNavEvent

    data class OpenConversationSettings(
        val conversationId: ConversationId,
    ) : ConversationListNavEvent
}
