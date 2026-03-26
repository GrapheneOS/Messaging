package com.android.messaging.ui.conversation.v2.metadata.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ConversationMetadataUiState {

    @Immutable
    data object Loading : ConversationMetadataUiState

    @Immutable
    data class Present(
        val title: String = "",
        val selfParticipantId: String = "",
        val isGroupConversation: Boolean = false,
        val participantCount: Int = 0,
    ) : ConversationMetadataUiState
}
