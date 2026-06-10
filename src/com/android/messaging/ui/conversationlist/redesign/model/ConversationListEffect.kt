package com.android.messaging.ui.conversationlist.redesign.model

internal sealed interface ConversationListEffect {

    data object StartChat : ConversationListEffect
    data object OpenArchivedConversations : ConversationListEffect
    data object OpenBlockedParticipants : ConversationListEffect
    data object OpenSettings : ConversationListEffect

    data class ConversationsArchived(
        val count: Int,
        val isArchived: Boolean,
    ) : ConversationListEffect

    data class OpenConversation(
        val conversationId: String,
    ) : ConversationListEffect

    data class ConfirmAddContact(
        val destination: String,
    ) : ConversationListEffect

    data class ConfirmBlock(
        val conversationId: String,
        val destination: String,
    ) : ConversationListEffect

    data class ConversationBlocked(
        val destination: String,
        val success: Boolean,
    ) : ConversationListEffect

    data object ScrollToTop : ConversationListEffect
}
