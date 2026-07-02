package com.android.messaging.ui.conversationlist.archived.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState

@Immutable
internal data class ArchivedConversationListUiState(
    val content: ConversationListContentUiState = ConversationListContentUiState.Loading,
    val selectedCount: Int = 0,
    val isDebugEnabled: Boolean = false,
)
