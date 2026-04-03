package com.android.messaging.ui.appsettings.screen.model

internal sealed interface SettingsAction {

    data class AutoRetrieveMmsChanged(val subId: Int, val enabled: Boolean) : SettingsAction

    data class AutoRetrieveMmsWhenRoamingChanged(val subId: Int, val enabled: Boolean) :
        SettingsAction

    data class DeliveryReportsChanged(val subId: Int, val enabled: Boolean) : SettingsAction

    data class GroupMmsChanged(val subId: Int, val enabled: Boolean) : SettingsAction

    data class PhoneNumberChanged(val subId: Int, val phoneNumber: String) : SettingsAction

    data class WirelessAlertsClicked(val subId: Int) : SettingsAction

    data class DumpMmsChanged(val enabled: Boolean) : SettingsAction

    data class DumpSmsChanged(val enabled: Boolean) : SettingsAction

    data class SendSoundChanged(val enabled: Boolean) : SettingsAction

    data class DefaultSmsAppClicked(val isCurrentlyDefault: Boolean) : SettingsAction

    data object NotificationsClicked : SettingsAction
    data object LicensesClicked : SettingsAction
}
