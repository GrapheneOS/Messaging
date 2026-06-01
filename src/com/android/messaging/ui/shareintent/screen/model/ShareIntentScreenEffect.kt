package com.android.messaging.ui.shareintent.screen.model

import kotlinx.collections.immutable.ImmutableSet

internal sealed interface ShareIntentScreenEffect {

    data class OpenConversation(
        val conversationId: String,
    ) : ShareIntentScreenEffect

    data object CreateNewConversation : ShareIntentScreenEffect

    data class SendToSelected(
        val conversationIds: ImmutableSet<String>,
    ) : ShareIntentScreenEffect
}
