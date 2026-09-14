package com.android.messaging.ui.appsettings.general.model

internal sealed interface AppSettingsScreenEffect {
    data object OpenManageDefaultApps : AppSettingsScreenEffect
    data object RequestDefaultSmsApp : AppSettingsScreenEffect
    data object OpenNotificationSettings : AppSettingsScreenEffect
}
