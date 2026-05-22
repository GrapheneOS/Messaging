package com.android.messaging.ui.blockedparticipants.screen.model

internal sealed interface BlockedParticipantsAction {

    data class UnblockClicked(
        val normalizedDestination: String,
    ) : BlockedParticipantsAction

    data class ParticipantClicked(
        val participantId: String,
    ) : BlockedParticipantsAction

    data class ParticipantLongClicked(
        val participantId: String,
    ) : BlockedParticipantsAction

    data object DeleteSelectedConfirmed : BlockedParticipantsAction
}
