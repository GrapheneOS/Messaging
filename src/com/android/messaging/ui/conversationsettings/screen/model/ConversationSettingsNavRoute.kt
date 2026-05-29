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

internal fun ConversationSettingsNavRoute.targetConversationId(
    rootConversationId: String,
): String {
    return when (this) {
        ConversationSettingsNavRoute.Conversation -> rootConversationId
        is ConversationSettingsNavRoute.ParticipantInfo -> conversationId
    }
}

internal fun ConversationSettingsNavRoute.saveableKey(): String {
    return when (this) {
        ConversationSettingsNavRoute.Conversation -> "conversation"
        is ConversationSettingsNavRoute.ParticipantInfo -> "participant:$conversationId"
    }
}
