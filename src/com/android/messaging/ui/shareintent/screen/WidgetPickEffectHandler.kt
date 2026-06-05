package com.android.messaging.ui.shareintent.screen

import com.android.messaging.ui.shareintent.screen.model.ShareIntentScreenEffect as Effect

internal class WidgetPickEffectHandler(
    private val onConversationPicked: (String) -> Unit,
) : ShareIntentEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenConversation -> onConversationPicked(effect.conversationId)
            else -> Unit
        }
    }
}
