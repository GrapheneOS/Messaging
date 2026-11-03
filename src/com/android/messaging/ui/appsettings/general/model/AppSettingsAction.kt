package com.android.messaging.ui.appsettings.general.model

import com.android.messaging.data.appsettings.model.LinkOpeningMode

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

    data class InConversationSoundChanged(
        val enabled: Boolean,
    ) : AppSettingsAction

    data class YouTubeLinkPreviewsChanged(
        val enabled: Boolean,
    ) : AppSettingsAction

    data class LinkOpeningModeChanged(
        val mode: LinkOpeningMode,
    ) : AppSettingsAction

    data class DefaultSmsAppClicked(
        val isCurrentlyDefault: Boolean,
    ) : AppSettingsAction
}
