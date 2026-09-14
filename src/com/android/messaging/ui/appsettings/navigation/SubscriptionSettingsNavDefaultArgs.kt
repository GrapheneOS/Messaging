package com.android.messaging.ui.appsettings.navigation

import android.os.Bundle
import com.android.messaging.ui.appsettings.subscription.SUBSCRIPTION_SETTINGS_SUB_ID_ARG

internal fun subscriptionSettingsDefaultArgs(navKey: SubscriptionSettingsNavKey): Bundle {
    return Bundle().apply {
        putInt(SUBSCRIPTION_SETTINGS_SUB_ID_ARG, navKey.subId.value)
    }
}
