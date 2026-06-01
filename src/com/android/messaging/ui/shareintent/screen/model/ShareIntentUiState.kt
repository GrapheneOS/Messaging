package com.android.messaging.ui.shareintent.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
internal data class ShareIntentUiState(
    val isLoading: Boolean = true,
    val targets: ImmutableList<ShareTargetUiState> = persistentListOf(),
    val isSearchActive: Boolean = false,
    val selectedConversationIds: ImmutableSet<String> = persistentSetOf(),
)

@Immutable
internal data class ShareTargetUiState(
    val conversationId: String,
    val displayName: String,
    val details: String?,
    val avatarUri: String?,
)
