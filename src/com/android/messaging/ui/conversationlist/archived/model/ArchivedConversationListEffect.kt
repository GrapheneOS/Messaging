package com.android.messaging.ui.conversationlist.archived.model

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.contact.model.AddContactRequest
import kotlinx.collections.immutable.ImmutableList

internal sealed interface ArchivedConversationListEffect {

    data object OpenDebugOptions : ArchivedConversationListEffect

    data class PlaceCall(
        val destination: String,
    ) : ArchivedConversationListEffect

    data class ShowContactCard(
        val contactId: Long,
        val contactLookupKey: String,
    ) : ArchivedConversationListEffect

    data class AddContact(
        val request: AddContactRequest,
    ) : ArchivedConversationListEffect

    data class ConversationsUnarchived(
        val conversationIds: ImmutableList<ConversationId>,
    ) : ArchivedConversationListEffect
}
