package com.android.messaging.ui.conversationlist.redesign

import com.android.messaging.ui.conversationlist.redesign.model.ConversationListEffect as Effect

internal interface ConversationListEffectHandler {
    fun handle(effect: Effect)
}
