package com.android.messaging.ui.conversationlist.redesign.model

import kotlinx.collections.immutable.ImmutableList

internal sealed interface ConversationListEffect {

    data object StartChat : ConversationListEffect
    data object OpenArchivedConversations : ConversationListEffect
    data object OpenBlockedParticipants : ConversationListEffect
    data object OpenSettings : ConversationListEffect
    data object OpenDebugOptions : ConversationListEffect

    data class ConversationsArchived(
        val conversationIds: ImmutableList<String>,
        val count: Int,
        val isArchived: Boolean,
    ) : ConversationListEffect

    data class OpenConversation(
        val conversationId: String,
    ) : ConversationListEffect

    data class PlaceCall(
        val destination: String,
    ) : ConversationListEffect

    data class ShowOrAddContact(
        val contactId: Long,
        val lookupKey: String?,
        val avatarUri: String?,
        val destination: String?,
    ) : ConversationListEffect

    data class ConfirmAddContact(
        val destination: String,
    ) : ConversationListEffect

    data class OpenAddContact(
        val destination: String,
    ) : ConversationListEffect

    data class ConfirmBlock(
        val conversationId: String,
        val destination: String,
    ) : ConversationListEffect

    data class ConversationBlocked(
        val conversationId: String,
        val destination: String,
        val success: Boolean,
    ) : ConversationListEffect

    data object ScrollToTop : ConversationListEffect
}
