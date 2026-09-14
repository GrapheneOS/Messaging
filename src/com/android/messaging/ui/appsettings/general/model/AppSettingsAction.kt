package com.android.messaging.ui.appsettings.general.model

internal sealed interface AppSettingsAction {

    data object NotificationsClicked : AppSettingsAction
    data object LicensesClicked : AppSettingsAction

    data class DumpMmsChanged(
        val enabled: Boolean,
    ) : AppSettingsAction

    data class DumpSmsChanged(
        val enabled: Boolean,
    ) : AppSettingsAction

    data class SendSoundChanged(
        val enabled: Boolean,
    ) : AppSettingsAction

    data class YouTubeLinkPreviewsChanged(
        val enabled: Boolean,
    ) : AppSettingsAction

    data class DefaultSmsAppClicked(
        val isCurrentlyDefault: Boolean,
    ) : AppSettingsAction
}
