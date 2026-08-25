package com.android.messaging.domain.conversation.usecase.participant

import javax.inject.Inject

internal fun interface IsConversationRecipientLimitExceeded {
    operator fun invoke(participantCount: Int): Boolean
}

internal class IsConversationRecipientLimitExceededImpl @Inject constructor() :
    IsConversationRecipientLimitExceeded {

    override operator fun invoke(participantCount: Int): Boolean {
        return participantCount > conversationRecipientLimit()
    }
}
