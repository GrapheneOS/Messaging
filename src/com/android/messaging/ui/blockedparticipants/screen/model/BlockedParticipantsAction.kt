package com.android.messaging.ui.blockedparticipants.screen.model

import com.android.messaging.data.conversation.model.ConversationId

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

    data class ParticipantMessageClicked(
        val conversationId: ConversationId,
    ) : BlockedParticipantsAction

    data class ParticipantCallClicked(
        val destination: String,
    ) : BlockedParticipantsAction

    data class ParticipantContactInfoClicked(
        val participant: BlockedParticipantUiState,
    ) : BlockedParticipantsAction

    data object DeleteSelectedConfirmed : BlockedParticipantsAction

    data object ClearSelectionClicked : BlockedParticipantsAction
}
