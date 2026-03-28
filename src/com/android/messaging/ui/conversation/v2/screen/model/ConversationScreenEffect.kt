package com.android.messaging.ui.conversation.v2.screen.model

internal sealed interface ConversationScreenEffect {
    data class LaunchAttachmentChooser(
        val conversationId: String,
    ) : ConversationScreenEffect
}
