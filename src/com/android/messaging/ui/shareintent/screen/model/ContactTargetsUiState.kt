package com.android.messaging.ui.shareintent.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ContactTargetsUiState(
    val sections: ImmutableList<ShareContactSection> = persistentListOf(),
    val hasPermission: Boolean = true,
    val canLoadMore: Boolean = false,
)
