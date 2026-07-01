package com.android.messaging.ui.conversationlist.chats.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationListSelectionUiState(
    val selectedCount: Int = 0,
    val actions: SelectionActionsUiState = SelectionActionsUiState(),
)

@Immutable
internal data class SelectionActionsUiState(
    val canAddContact: Boolean = false,
    val canBlock: Boolean = false,
    val firstSelectedIsPinned: Boolean? = null,
    val firstSelectedIsSnoozed: Boolean? = null,
    val firstSelectedIsUnread: Boolean? = null,
)
