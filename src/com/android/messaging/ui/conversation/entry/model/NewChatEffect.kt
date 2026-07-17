package com.android.messaging.ui.conversation.entry.model

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId

internal sealed interface NewChatEffect {

    data class NavigateToConversation(
        val conversationId: ConversationId,
        val selfParticipantId: ParticipantId?,
    ) : NewChatEffect

    data object NavigateBack : NewChatEffect

    data class ShowMessage(
        val messageResId: Int,
    ) : NewChatEffect
}
