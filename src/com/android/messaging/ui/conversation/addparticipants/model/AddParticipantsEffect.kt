package com.android.messaging.ui.conversation.addparticipants.model

import com.android.messaging.data.conversation.model.ConversationId

internal sealed interface AddParticipantsEffect {

    data class NavigateToConversation(
        val conversationId: ConversationId,
    ) : AddParticipantsEffect

    data class ShowMessage(
        val messageResId: Int,
    ) : AddParticipantsEffect
}
