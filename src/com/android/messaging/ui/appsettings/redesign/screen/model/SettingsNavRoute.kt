package com.android.messaging.ui.appsettings.redesign.screen.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface SettingsNavRoute {
    data object Main : SettingsNavRoute
    data object AppSettings : SettingsNavRoute
    data class SubscriptionSettings(val subId: Int, val title: String) : SettingsNavRoute
}
