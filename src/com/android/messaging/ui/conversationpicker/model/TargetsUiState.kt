package com.android.messaging.ui.conversationpicker.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class TargetsUiState(
    val isLoading: Boolean = true,
    val isSearchActive: Boolean = false,
    val recent: RecentTargetsUiState = RecentTargetsUiState(),
    val selection: SelectionUiState = SelectionUiState(),
)
