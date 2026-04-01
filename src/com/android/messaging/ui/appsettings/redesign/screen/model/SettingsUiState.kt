package com.android.messaging.ui.appsettings.redesign.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.appsettings.redesign.appsettings.model.AppSettingsUiState
import com.android.messaging.ui.appsettings.redesign.subscription.model.SubscriptionSettingsUiState

@Immutable
internal data class SettingsUiState(
    val isMultiSim: Boolean = false,
    val subscriptionSettings: List<SubscriptionSettingsUiState> = emptyList(),
    val appSettings: AppSettingsUiState = AppSettingsUiState(),
)
