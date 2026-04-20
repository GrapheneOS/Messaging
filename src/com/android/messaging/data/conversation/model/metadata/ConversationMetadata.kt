package com.android.messaging.data.conversation.model.metadata

internal data class ConversationMetadata(
    val conversationName: String,
    val selfParticipantId: String,
    val isGroupConversation: Boolean,
    val participantCount: Int,
    val otherParticipantDisplayDestination: String?,
    val otherParticipantNormalizedDestination: String?,
    val otherParticipantContactLookupKey: String?,
    val otherParticipantPhotoUri: String?,
    val isArchived: Boolean,
    val composerAvailability: ConversationComposerAvailability,
)
