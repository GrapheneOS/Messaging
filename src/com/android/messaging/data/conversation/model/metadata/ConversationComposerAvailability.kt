package com.android.messaging.data.conversation.model.metadata

internal sealed interface ConversationComposerAvailability {
    data object Editable : ConversationComposerAvailability

    data class Unavailable(
        val reason: ConversationComposerDisabledReason,
    ) : ConversationComposerAvailability
}
