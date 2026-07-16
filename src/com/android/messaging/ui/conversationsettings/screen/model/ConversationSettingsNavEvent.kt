package com.android.messaging.ui.conversationsettings.screen.model

import com.android.messaging.data.conversation.model.ConversationId

internal sealed interface ConversationSettingsNavEvent {

    data class OpenParticipantInfo(
        val conversationId: ConversationId,
    ) : ConversationSettingsNavEvent

    data object CloseAfterArchive : ConversationSettingsNavEvent
}
