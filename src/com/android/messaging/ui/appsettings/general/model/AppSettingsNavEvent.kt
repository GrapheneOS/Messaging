package com.android.messaging.ui.appsettings.general.model

internal sealed interface AppSettingsNavEvent {
    data object OpenLicenses : AppSettingsNavEvent
}
