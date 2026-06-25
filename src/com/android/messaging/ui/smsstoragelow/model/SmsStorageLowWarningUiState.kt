package com.android.messaging.ui.smsstoragelow.model

import androidx.compose.runtime.Immutable
import com.android.messaging.domain.smsstoragelow.model.SmsStorageLowWarningAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SmsStorageLowWarningUiState(
    val actions: ImmutableList<SmsStorageLowWarningAction> = persistentListOf(),
    val selectedAction: SmsStorageLowWarningAction? = null,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
)
