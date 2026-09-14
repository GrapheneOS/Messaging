package com.android.messaging.ui.blockedparticipants.screen.model

import com.android.messaging.data.conversation.model.ConversationId

internal sealed interface BlockedParticipantsNavEvent {

    data object CloseAfterLastUnblock : BlockedParticipantsNavEvent

    data class OpenParticipantChat(
        val conversationId: ConversationId,
    ) : BlockedParticipantsNavEvent
}
