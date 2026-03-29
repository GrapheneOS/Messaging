package com.android.messaging.ui.appsettings.redesign.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface SettingsNavRoute {
    data object Main : SettingsNavRoute
    data object AppSettings : SettingsNavRoute
    data object SubscriptionSettings : SettingsNavRoute
}
