package com.android.messaging.data.conversation.model.metadata

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationSubscription(
    val selfParticipantId: String,
    val label: ConversationSubscriptionLabel,
    val displayDestination: String?,
    val displaySlotId: Int,
    val color: Int,
)
