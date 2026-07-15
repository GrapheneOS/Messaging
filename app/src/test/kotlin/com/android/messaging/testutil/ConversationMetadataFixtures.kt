package com.android.messaging.testutil

import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationMetadata

internal fun createConversationMetadata(
    isGroupConversation: Boolean = false,
    includeEmailAddress: Boolean = false,
): ConversationMetadata {
    return ConversationMetadata(
        conversationName = "Conversation",
        selfParticipantId = "self-1",
        isGroupConversation = isGroupConversation,
        includeEmailAddress = includeEmailAddress,
        participantCount = 1,
        otherParticipantDisplayDestination = "Alice",
        otherParticipantNormalizedDestination = "123",
        otherParticipantContactLookupKey = null,
        otherParticipantPhotoUri = null,
        isArchived = false,
        isBlocked = false,
        composerAvailability = ConversationComposerAvailability.Editable,
        sortTimestamp = 0L,
    )
}
