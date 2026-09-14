package com.android.messaging.domain.conversation.usecase.participant

import javax.inject.Inject

internal fun interface CanAddMoreConversationParticipants {
    operator fun invoke(participantCount: Int): Boolean
}

internal class CanAddMoreConversationParticipantsImpl @Inject constructor() :
    CanAddMoreConversationParticipants {

    override operator fun invoke(participantCount: Int): Boolean {
        return participantCount < conversationRecipientLimit()
    }
}
