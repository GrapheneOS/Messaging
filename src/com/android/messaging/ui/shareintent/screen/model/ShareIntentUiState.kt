package com.android.messaging.ui.shareintent.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ShareIntentUiState(
    val isLoading: Boolean = true,
    val targets: ImmutableList<ShareTargetUiState> = persistentListOf(),
)

@Immutable
internal data class ShareTargetUiState(
    val conversationId: String,
)
