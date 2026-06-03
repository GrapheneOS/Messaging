package com.android.messaging.ui.shareintent.screen.model

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import kotlinx.collections.immutable.ImmutableSet

internal sealed interface ShareIntentScreenEffect {

    data class OpenConversation(
        val conversationId: String,
    ) : ShareIntentScreenEffect

    data object CreateNewConversation : ShareIntentScreenEffect

    data class SendToSelected(
        val conversationIds: ImmutableSet<String>,
        val draft: ConversationDraft,
    ) : ShareIntentScreenEffect
}
