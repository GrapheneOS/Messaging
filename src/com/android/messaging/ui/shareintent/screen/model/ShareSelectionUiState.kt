package com.android.messaging.ui.shareintent.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
internal data class ShareSelectionUiState(
    val selectedIds: ImmutableSet<String> = persistentSetOf(),
    val selectedTargets: ImmutableList<ShareTargetUiState> = persistentListOf(),
)
