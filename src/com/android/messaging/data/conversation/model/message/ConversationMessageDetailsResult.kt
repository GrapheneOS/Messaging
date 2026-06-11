package com.android.messaging.data.conversation.model.message

import com.android.messaging.datamodel.data.ConversationMessageData

internal data class ConversationMessageDetailsResult(
    val message: ConversationMessageData,
    val details: ConversationMessageDetails,
)
