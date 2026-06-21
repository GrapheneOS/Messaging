package com.android.messaging.ui.conversationlist.redesign.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ConversationListSelectionUiState(
    val selectedConversations: ImmutableList<SelectedConversationUiModel> = persistentListOf(),
    val actions: SelectionActionsUiState = SelectionActionsUiState(),
    val isActive: Boolean = false,
)

@Immutable
internal data class SelectedConversationUiModel(
    val conversationId: String,
    val normalizedDestination: String?,
    val participantLookupKey: String?,
    val isGroup: Boolean,
    val isArchived: Boolean,
    val isSnoozed: Boolean,
    val isUnread: Boolean,
)

@Immutable
internal data class SelectionActionsUiState(
    val canArchive: Boolean = false,
    val canUnarchive: Boolean = false,
    val canDelete: Boolean = false,
    val canAddContact: Boolean = false,
    val canBlock: Boolean = false,
    val canSnooze: Boolean = false,
    val canUnsnooze: Boolean = false,
    val isFirstSelectedUnread: Boolean? = null,
)
