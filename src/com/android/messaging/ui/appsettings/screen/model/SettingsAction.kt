package com.android.messaging.ui.appsettings.screen.model

internal sealed interface SettingsAction {

    sealed interface Subscription : SettingsAction

    data class AutoRetrieveMmsChanged(
        val subId: Int,
        val enabled: Boolean,
    ) : Subscription

    data class AutoRetrieveMmsWhenRoamingChanged(
        val subId: Int,
        val enabled: Boolean,
    ) : Subscription

    data class DeliveryReportsChanged(
        val subId: Int,
        val enabled: Boolean,
    ) : Subscription

    data class GroupMmsChanged(
        val subId: Int,
        val enabled: Boolean,
    ) : Subscription

    data class PhoneNumberChanged(
        val subId: Int,
        val phoneNumber: String,
    ) : Subscription

    data class WirelessAlertsClicked(
        val subId: Int,
    ) : SettingsAction

    data class DumpMmsChanged(
        val enabled: Boolean,
    ) : SettingsAction

    data class DumpSmsChanged(
        val enabled: Boolean,
    ) : SettingsAction

    data class SendSoundChanged(
        val enabled: Boolean,
    ) : SettingsAction

    data class YouTubeLinkPreviewsChanged(
        val enabled: Boolean,
    ) : SettingsAction

    data class DefaultSmsAppClicked(
        val isCurrentlyDefault: Boolean,
    ) : SettingsAction

    data object NotificationsClicked : SettingsAction
    data object LicensesClicked : SettingsAction
}
