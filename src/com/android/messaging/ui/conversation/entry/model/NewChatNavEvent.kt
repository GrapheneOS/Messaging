package com.android.messaging.ui.conversation.entry.model

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId

internal sealed interface NewChatNavEvent {

    data object Close : NewChatNavEvent

    data class OpenConversation(
        val conversationId: ConversationId,
        val selfParticipantId: ParticipantId?,
    ) : NewChatNavEvent
}
