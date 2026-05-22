package com.android.messaging.ui.blockedparticipants.screen.model

internal sealed interface BlockedParticipantsScreenEffect {

    data class ShowMessage(
        val messageResId: Int,
    ) : BlockedParticipantsScreenEffect

    data class OpenParticipantChat(
        val normalizedDestination: String,
    ) : BlockedParticipantsScreenEffect
}
