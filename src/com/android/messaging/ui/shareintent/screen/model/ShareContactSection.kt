package com.android.messaging.ui.shareintent.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ShareContactSection(
    val label: String,
    val targets: ImmutableList<ShareTargetUiState.Contact> = persistentListOf(),
)
