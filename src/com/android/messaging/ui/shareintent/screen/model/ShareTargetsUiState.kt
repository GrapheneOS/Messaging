package com.android.messaging.ui.shareintent.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
internal data class ShareTargetsUiState(
    val isLoading: Boolean = true,
    val recentTargets: ImmutableList<ShareTargetUiState> = persistentListOf(),
    val contactSections: ImmutableList<ShareContactSection> = persistentListOf(),
    val canLoadMoreRecent: Boolean = false,
    val canCollapseRecent: Boolean = false,
    val hasContactsPermission: Boolean = true,
    val canLoadMoreContacts: Boolean = false,
    val isSearchActive: Boolean = false,
    val selectedIds: ImmutableSet<String> = persistentSetOf(),
    val selectedTargets: ImmutableList<ShareTargetUiState> = persistentListOf(),
)
