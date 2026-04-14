package com.android.messaging.ui.conversation.v2.entry.model

internal sealed interface ConversationEntryEffect {

    data class NavigateToConversation(
        val conversationId: String,
    ) : ConversationEntryEffect

    data object NavigateBack : ConversationEntryEffect

    data class ShowMessage(
        val messageResId: Int,
    ) : ConversationEntryEffect
}
