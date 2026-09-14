package com.android.messaging.ui.conversation.navigation

import android.content.Intent
import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.entry.hasConversationLaunchPayload

internal fun conversationRoute(intent: Intent): List<NavKey>? {
    val conversationId = intent
        .getStringExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID)
        .let(ConversationId::fromOrNull)

    return when {
        conversationId != null -> {
            listOf(ConversationNavKey(conversationId))
        }

        intent.isComposeNewConversation() || intent.hasConversationLaunchPayload() -> {
            listOf(NewChatNavKey)
        }

        else -> null
    }
}

private fun Intent.isComposeNewConversation(): Boolean {
    return getBooleanExtra(UIIntents.UI_INTENT_EXTRA_COMPOSE_NEW_CONVERSATION, false)
}
