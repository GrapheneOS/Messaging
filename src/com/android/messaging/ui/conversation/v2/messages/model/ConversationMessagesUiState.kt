package com.android.messaging.ui.conversation.v2.messages.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ConversationMessagesUiState {

    @Immutable
    data object Loading : ConversationMessagesUiState

    @Immutable
    data class Present(
        val messages: List<ConversationMessageUiModel> = emptyList(),
    ) : ConversationMessagesUiState
}
