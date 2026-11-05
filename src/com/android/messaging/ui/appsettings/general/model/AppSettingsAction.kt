package com.android.messaging.ui.appsettings.general.model

import com.android.messaging.data.appsettings.model.ConversationSwipeOption

internal sealed interface AppSettingsAction {

    data object NotificationsClicked : AppSettingsAction
    data object LicensesClicked : AppSettingsAction

    data class DumpMmsChanged(
        val enabled: Boolean,
    ) : AppSettingsAction

    data class DumpSmsChanged(
        val enabled: Boolean,
    ) : AppSettingsAction

    data class ConversationSwipeStartToEndOptionChanged(
        val option: ConversationSwipeOption,
    ) : AppSettingsAction

    data class ConversationSwipeEndToStartOptionChanged(
        val option: ConversationSwipeOption,
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

    data class DefaultSmsAppClicked(
        val isCurrentlyDefault: Boolean,
    ) : AppSettingsAction
}
