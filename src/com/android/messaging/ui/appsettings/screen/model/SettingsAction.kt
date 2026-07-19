package com.android.messaging.ui.appsettings.screen.model

import com.android.messaging.data.subscription.model.SubId

internal sealed interface SettingsAction {

    data class AutoRetrieveMmsChanged(
        val subId: SubId,
        val enabled: Boolean,
    ) : SettingsAction

    data class AutoRetrieveMmsWhenRoamingChanged(
        val subId: SubId,
        val enabled: Boolean,
    ) : SettingsAction

    data class DeliveryReportsChanged(
        val subId: SubId,
        val enabled: Boolean,
    ) : SettingsAction

    data class GroupMmsChanged(
        val subId: SubId,
        val enabled: Boolean,
    ) : SettingsAction

    data class PhoneNumberChanged(
        val subId: SubId,
        val phoneNumber: String,
    ) : SettingsAction

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

    data class DefaultSmsAppClicked(
        val isCurrentlyDefault: Boolean,
    ) : SettingsAction

    data object NotificationsClicked : SettingsAction
    data object LicensesClicked : SettingsAction
}
