package com.android.messaging.data.conversation.repository

import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import kotlinx.collections.immutable.ImmutableList

internal data class ConversationRecipientsPage(
    val recipients: ImmutableList<ConversationRecipient>,
    val nextOffset: Int?,
)
