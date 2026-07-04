package com.android.messaging.ui.vcarddetail.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.vcarddetail.model.VCardContact
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class VCardDetailUiState(
    val contacts: ImmutableList<VCardContact> = persistentListOf(),
    val canAddToContacts: Boolean = false,
    val isLoading: Boolean = true,
)
