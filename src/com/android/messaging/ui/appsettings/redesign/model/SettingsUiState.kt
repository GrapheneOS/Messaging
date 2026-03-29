package com.android.messaging.ui.appsettings.redesign.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.appsettings.redesign.subscription.model.SubscriptionSettingsUiState

@Immutable
internal data class SettingsUiState(
    val isMultiSim: Boolean = false,
    val subscriptionSettings: List<SubscriptionSettingsUiState> = emptyList(),
)
