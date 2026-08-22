package com.android.messaging.ui.appsettings.screen.model

internal sealed interface SettingsNavEvent {
    data object OpenLicenses : SettingsNavEvent
}
