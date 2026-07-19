package com.android.messaging.ui.appsettings.screen.model

import com.android.messaging.data.subscription.model.SubId

internal sealed interface SettingsScreenEffect {
    data class OpenWirelessAlerts(
        val subId: SubId,
    ) : SettingsScreenEffect

    data object OpenManageDefaultApps : SettingsScreenEffect
    data object RequestDefaultSmsApp : SettingsScreenEffect
    data object OpenNotificationSettings : SettingsScreenEffect
    data object OpenLicenses : SettingsScreenEffect
}
