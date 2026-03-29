package com.android.messaging.ui.appsettings.redesign.screen.model

internal sealed interface SettingsScreenEffect {
    data class OpenWirelessAlerts(val subId: Int) : SettingsScreenEffect
}
