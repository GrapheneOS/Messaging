package com.android.messaging.ui.conversation.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState

@Immutable
internal data class ConversationScreenScaffoldUiState(
    val canAddPeople: Boolean = false,
    val canCall: Boolean = false,
    val canArchive: Boolean = false,
    val canUnarchive: Boolean = false,
    val canAddContact: Boolean = false,
    val canDeleteConversation: Boolean = false,
    val isDeleteConversationConfirmationVisible: Boolean = false,
    val metadata: ConversationMetadataUiState = ConversationMetadataUiState.Loading,
    val messages: ConversationMessagesUiState = ConversationMessagesUiState.Loading,
    val composer: ConversationComposerUiState = ConversationComposerUiState(),
    val selection: ConversationMessageSelectionUiState = ConversationMessageSelectionUiState(),
)
