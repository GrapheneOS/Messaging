package com.android.messaging.ui.shareintent.screen.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ShareTargetsUiState(
    val isLoading: Boolean = true,
    val isSearchActive: Boolean = false,
    val recent: RecentTargetsUiState = RecentTargetsUiState(),
    val contacts: ContactTargetsUiState = ContactTargetsUiState(),
    val selection: ShareSelectionUiState = ShareSelectionUiState(),
)
