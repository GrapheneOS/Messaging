package com.android.messaging.ui.conversation.v2.composer.delegate

internal sealed interface ConversationDraftEffect {
    data class LaunchAttachmentChooser(
        val conversationId: String,
    ) : ConversationDraftEffect
}
