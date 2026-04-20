package com.android.messaging.data.conversation.model.metadata

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ConversationSubscriptionLabel {

    @Immutable
    data class Named(
        val name: String,
    ) : ConversationSubscriptionLabel

    @Immutable
    data class Slot(
        val slotId: Int,
    ) : ConversationSubscriptionLabel

    @Immutable
    data class DebugFake(
        val slotId: Int,
    ) : ConversationSubscriptionLabel
}
