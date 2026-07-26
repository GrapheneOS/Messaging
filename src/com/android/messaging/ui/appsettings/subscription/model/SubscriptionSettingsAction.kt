package com.android.messaging.ui.appsettings.subscription.model

internal sealed interface SubscriptionSettingsAction {

    data object WirelessAlertsClicked : SubscriptionSettingsAction

    data class AutoRetrieveMmsChanged(
        val enabled: Boolean,
    ) : SubscriptionSettingsAction

    data class AutoRetrieveMmsWhenRoamingChanged(
        val enabled: Boolean,
    ) : SubscriptionSettingsAction

    data class DeliveryReportsChanged(
        val enabled: Boolean,
    ) : SubscriptionSettingsAction

    data class GroupMmsChanged(
        val enabled: Boolean,
    ) : SubscriptionSettingsAction

    data class PhoneNumberChanged(
        val phoneNumber: String,
    ) : SubscriptionSettingsAction
}
