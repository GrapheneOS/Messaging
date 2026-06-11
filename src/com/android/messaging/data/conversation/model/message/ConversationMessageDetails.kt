package com.android.messaging.data.conversation.model.message

import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import kotlinx.collections.immutable.ImmutableList

internal data class ConversationMessageDetails(
    val type: Type,
    val sender: String?,
    val recipients: ImmutableList<String>,
    val sentTimestamp: Long?,
    val receivedTimestamp: Long?,
    val priority: Priority?,
    val sizeBytes: Long?,
    val subscriptionLabel: ConversationSubscriptionLabel?,
    val debug: Debug?,
) {
    enum class Type {
        SMS,
        MMS,
    }

    enum class Priority {
        LOW,
        NORMAL,
        HIGH,
    }

    data class Debug(
        val messageId: String?,
        val telephonyUri: String?,
        val conversationId: String?,
        val conversationTelephonyThreadId: Long?,
        val telephonyThreadId: Long?,
        val contentLocationUrl: String?,
        val threadRecipientIds: String?,
        val threadRecipients: String?,
        val sender: String?,
    )
}
