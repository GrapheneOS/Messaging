package com.android.messaging.ui.conversationlist.archived.model

import com.android.messaging.data.conversation.model.ConversationId

internal sealed interface ArchivedConversationListNavEvent {

    data class OpenConversation(
        val conversationId: ConversationId,
    ) : ArchivedConversationListNavEvent

    data class OpenConversationSettings(
        val conversationId: ConversationId,
    ) : ArchivedConversationListNavEvent
}
