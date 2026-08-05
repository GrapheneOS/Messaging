package com.android.messaging.domain.conversationpicker.model

import com.android.messaging.data.conversation.model.ConversationId

internal sealed interface SendTarget {

    data class Conversation(
        val conversationId: ConversationId,
    ) : SendTarget

    data class Contact(
        val destination: String,
    ) : SendTarget
}
