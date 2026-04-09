package com.android.messaging.ui.conversation.v2.messages.model.message

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed interface ConversationMessagesUiState {

    @Immutable
    data object Loading : ConversationMessagesUiState

    @Immutable
    data class Present(
        val messages: ImmutableList<ConversationMessageUiModel> = persistentListOf(),
    ) : ConversationMessagesUiState
}
