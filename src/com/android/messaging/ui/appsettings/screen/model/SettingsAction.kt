package com.android.messaging.ui.appsettings.screen.model

import com.android.messaging.data.subscription.model.SubId

internal sealed interface SettingsAction {

    sealed interface Subscription : SettingsAction

    data class AutoRetrieveMmsChanged(
        val subId: SubId,
        val enabled: Boolean,
    ) : Subscription

    data class AutoRetrieveMmsWhenRoamingChanged(
        val subId: SubId,
        val enabled: Boolean,
    ) : Subscription

    data class DeliveryReportsChanged(
        val subId: SubId,
        val enabled: Boolean,
    ) : Subscription

    data class GroupMmsChanged(
        val subId: SubId,
        val enabled: Boolean,
    ) : Subscription

    data class PhoneNumberChanged(
        val subId: SubId,
        val phoneNumber: String,
    ) : Subscription

    data class WirelessAlertsClicked(
        val subId: SubId,
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
