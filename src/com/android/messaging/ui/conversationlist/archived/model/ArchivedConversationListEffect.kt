package com.android.messaging.ui.conversationlist.archived.model

import kotlinx.collections.immutable.ImmutableList

internal sealed interface ArchivedConversationListEffect {

    data object OpenDebugOptions : ArchivedConversationListEffect

    data class OpenConversation(
        val conversationId: String,
    ) : ArchivedConversationListEffect

    data class OpenConversationSettings(
        val conversationId: String,
    ) : ArchivedConversationListEffect

    data class PlaceCall(
        val destination: String,
    ) : ArchivedConversationListEffect

    data class ShowOrAddContact(
        val contactId: Long,
        val lookupKey: String?,
        val avatarUri: String?,
        val destination: String?,
    ) : ArchivedConversationListEffect

    data class ConversationsUnarchived(
        val conversationIds: ImmutableList<String>,
    ) : ArchivedConversationListEffect
}
