package com.android.messaging.ui.conversationlist.chats.model

import com.android.messaging.data.conversation.model.ConversationId
import kotlinx.collections.immutable.ImmutableList

internal sealed interface ConversationListEffect {

    data object OpenSettings : ConversationListEffect
    data object OpenDebugOptions : ConversationListEffect
    data object ScrollToTop : ConversationListEffect

    data class ArchiveStatusChanged(
        val conversationIds: ImmutableList<ConversationId>,
        val isArchived: Boolean,
    ) : ConversationListEffect

    data class PreparePinAnimation(
        val conversationIds: ImmutableList<ConversationId>,
        val isPinned: Boolean,
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

    data class ConfirmBlock(
        val conversationId: ConversationId,
        val destination: String,
    ) : ConversationListEffect

    data class ConversationBlocked(
        val conversationId: ConversationId,
        val destination: String,
        val success: Boolean,
    ) : ConversationListEffect
}
