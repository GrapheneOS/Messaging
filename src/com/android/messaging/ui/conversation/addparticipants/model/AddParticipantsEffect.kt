package com.android.messaging.ui.conversation.addparticipants.model

internal sealed interface AddParticipantsEffect {

    data class NavigateToConversation(
        val conversationId: String,
    ) : AddParticipantsEffect

    data class ShowMessage(
        val messageResId: Int,
    ) : AddParticipantsEffect
}
