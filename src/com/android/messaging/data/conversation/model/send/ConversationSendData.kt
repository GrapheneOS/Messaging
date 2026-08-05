package com.android.messaging.data.conversation.model.send

import com.android.messaging.data.conversation.model.metadata.ConversationMetadata
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.ParticipantData

internal data class ConversationSendData(
    val metadata: ConversationMetadata,
    val participants: ConversationParticipantsData,
    val selfParticipant: ParticipantData?,
) {
    val selfSubId: SubId
        get() = SubId(selfParticipant?.subId ?: ParticipantData.DEFAULT_SELF_SUB_ID)
}
