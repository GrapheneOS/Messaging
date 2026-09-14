package com.android.messaging.domain.conversation.usecase.participant

import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.sms.MmsConfig

internal fun conversationRecipientLimit(): Int {
    return MmsConfig.get(ParticipantData.DEFAULT_SELF_SUB_ID).recipientLimit
}
