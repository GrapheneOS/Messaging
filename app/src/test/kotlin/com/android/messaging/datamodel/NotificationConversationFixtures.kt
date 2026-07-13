package com.android.messaging.datamodel

import com.android.messaging.datamodel.data.ParticipantData

internal fun createNotificationConversation(
    conversationId: String,
    conversationName: String = "Conversation",
    selfParticipantId: String = "self-1",
    receivedTimestamp: Long = 0L,
    isGroup: Boolean = false,
    participantCount: Int = 2,
): MessageNotificationState.Conversation {
    return MessageNotificationState.Conversation(
        conversationId,
        isGroup,
        conversationName,
        false,
        receivedTimestamp,
        selfParticipantId,
        null,
        true,
        false,
        ParticipantData.DEFAULT_SELF_SUB_ID,
        participantCount,
        null,
    )
}
