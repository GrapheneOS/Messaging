package com.android.messaging.ui.appsettings.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SettingsUiState(
    val isMultiSim: Boolean? = null,
    val areSubscriptionsLoaded: Boolean = false,
    val subscriptionSettings: ImmutableList<SubscriptionSettingsUiState> = persistentListOf(),
    val appSettings: AppSettingsUiState = AppSettingsUiState(),
)
