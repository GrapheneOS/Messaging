package com.android.messaging.domain.conversation.usecase

import com.android.messaging.datamodel.data.ContactPickerData
import javax.inject.Inject

internal fun interface IsConversationRecipientLimitExceeded {
    operator fun invoke(participantCount: Int): Boolean
}

internal class IsConversationRecipientLimitExceededImpl @Inject constructor() :
    IsConversationRecipientLimitExceeded {

    override operator fun invoke(participantCount: Int): Boolean {
        return ContactPickerData.isTooManyParticipants(participantCount)
    }
}
