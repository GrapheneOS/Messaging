package com.android.messaging.ui.conversation.addparticipants.model

import com.android.messaging.data.conversation.model.ConversationId

internal sealed interface AddParticipantsNavEvent {

    data class OpenConversation(
        val conversationId: ConversationId,
    ) : AddParticipantsNavEvent
}
