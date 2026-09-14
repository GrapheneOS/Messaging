package com.android.messaging.ui.appsettings.subscription.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class PhoneNumberDialogUiState(
    val isVisible: Boolean = false,
    val isInvalid: Boolean = false,
)
