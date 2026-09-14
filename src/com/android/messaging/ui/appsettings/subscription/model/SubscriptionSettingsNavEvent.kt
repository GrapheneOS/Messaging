package com.android.messaging.ui.appsettings.subscription.model

internal sealed interface SubscriptionSettingsNavEvent {
    data object Close : SubscriptionSettingsNavEvent
}
