package com.android.messaging.ui.conversationsettings.screen.model

internal sealed interface ConversationSettingsNavEvent {

    data class OpenParticipantInfo(
        val conversationId: String,
    ) : ConversationSettingsNavEvent

    data object CloseAfterArchive : ConversationSettingsNavEvent
}
