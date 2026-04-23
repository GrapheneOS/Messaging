package com.android.messaging.ui.conversation.v2.recipientpicker.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class RecipientPickerUiState(
    val query: String = "",
    val items: ImmutableList<RecipientPickerListItem> = persistentListOf(),
    val canLoadMore: Boolean = false,
    val hasContactsPermission: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
)
