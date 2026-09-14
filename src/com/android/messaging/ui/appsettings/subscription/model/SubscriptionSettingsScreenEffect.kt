package com.android.messaging.ui.appsettings.subscription.model

internal sealed interface SubscriptionSettingsScreenEffect {
    data object OpenWirelessAlerts : SubscriptionSettingsScreenEffect
}
