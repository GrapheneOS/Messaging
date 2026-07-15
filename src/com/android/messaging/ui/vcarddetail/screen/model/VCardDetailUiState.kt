package com.android.messaging.ui.vcarddetail.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class VCardDetailUiState(
    val contacts: ImmutableList<VCardContactUiModel> = persistentListOf(),
    val canAddToContacts: Boolean = false,
    val isLoading: Boolean = true,
)
