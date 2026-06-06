package com.android.messaging.ui.shareintent.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class RecentTargetsUiState(
    val targets: ImmutableList<ShareTargetUiState> = persistentListOf(),
    val canLoadMore: Boolean = false,
    val canCollapse: Boolean = false,
)
