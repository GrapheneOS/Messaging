package com.android.messaging.ui.conversationsettings.screen.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ConversationSettingsNavRoute {

    val depth: Int

    data object Conversation : ConversationSettingsNavRoute {
        override val depth: Int = 0
    }

    data class ParticipantInfo(
        val conversationId: String,
    ) : ConversationSettingsNavRoute {
        override val depth: Int = 1
    }
}
