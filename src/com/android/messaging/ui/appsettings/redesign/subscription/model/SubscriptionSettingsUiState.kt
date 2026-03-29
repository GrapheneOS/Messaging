package com.android.messaging.ui.appsettings.redesign.subscription.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class SubscriptionSettingsUiState(
    val subId: Int = -1,
    val displayName: String = "",
    val displayDetail: String = "",
)
