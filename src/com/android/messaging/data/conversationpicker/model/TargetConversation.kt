package com.android.messaging.data.conversationpicker.model

import com.android.messaging.data.conversation.model.ConversationId

internal data class TargetConversation(
    val conversationId: ConversationId,
    val name: String,
    val icon: String?,
    val normalizedDestination: String?,
    val isGroup: Boolean,
)
