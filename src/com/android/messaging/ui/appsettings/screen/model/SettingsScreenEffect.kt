package com.android.messaging.ui.appsettings.screen.model

internal sealed interface SettingsScreenEffect {
    data class OpenWirelessAlerts(val subId: Int) : SettingsScreenEffect
    data object OpenManageDefaultApps : SettingsScreenEffect
    data object RequestDefaultSmsApp : SettingsScreenEffect
    data object OpenNotificationSettings : SettingsScreenEffect
    data object OpenLicenses : SettingsScreenEffect
}
