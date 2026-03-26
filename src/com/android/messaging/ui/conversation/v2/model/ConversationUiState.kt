package com.android.messaging.ui.conversation.v2.screen

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState

@Immutable
internal data class ConversationUiState(
    val metadata: ConversationMetadataUiState = ConversationMetadataUiState.Loading,
    val messages: ConversationMessagesUiState = ConversationMessagesUiState.Loading,
)
