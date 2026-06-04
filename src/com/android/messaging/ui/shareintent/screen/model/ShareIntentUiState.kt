package com.android.messaging.ui.shareintent.screen.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ShareIntentUiState(
    val targets: ShareTargetsUiState = ShareTargetsUiState(),
    val draft: ShareDraftUiState = ShareDraftUiState(),
    val isSendEnabled: Boolean = false,
) {
    val isLoading: Boolean
        get() = targets.isLoading || draft.isLoading
}
