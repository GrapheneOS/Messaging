package com.android.messaging.data.conversationlist.model

internal sealed interface ConversationListMessageStatus {
    data object Unknown : ConversationListMessageStatus
    data object Normal : ConversationListMessageStatus
    data object Sending : ConversationListMessageStatus
    data object Draft : ConversationListMessageStatus

    data class Failed(
        val rawTelephonyStatus: Int,
    ) : ConversationListMessageStatus
}
