package com.android.messaging.ui.appsettings.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState

@Immutable
internal data class SettingsUiState(
    val isMultiSim: Boolean? = null,
    val subscriptionSettings: List<SubscriptionSettingsUiState> = emptyList(),
    val appSettings: AppSettingsUiState = AppSettingsUiState(),
)
