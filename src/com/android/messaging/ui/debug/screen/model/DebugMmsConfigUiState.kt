package com.android.messaging.ui.debug.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.subscription.model.SubId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class DebugMmsConfigUiState(
    val items: ImmutableList<MmsConfigItemUiState> = persistentListOf(),
    val sims: ImmutableList<DebugSimUiState> = persistentListOf(),
    val selectedSubId: SubId? = null,
    val isLoading: Boolean = true,
)
