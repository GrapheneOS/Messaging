package com.android.messaging.ui.conversation.v2.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState

@Immutable
internal data class ConversationScreenScaffoldUiState(
    val metadata: ConversationMetadataUiState = ConversationMetadataUiState.Loading,
    val messages: ConversationMessagesUiState = ConversationMessagesUiState.Loading,
    val composer: ConversationComposerUiState = ConversationComposerUiState(),
)
