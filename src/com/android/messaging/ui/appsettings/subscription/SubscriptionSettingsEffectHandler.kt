package com.android.messaging.ui.appsettings.subscription

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsScreenEffect as Effect
import com.android.messaging.util.LogUtil

@Composable
internal fun rememberSubscriptionSettingsEffectHandler(): SubscriptionSettingsEffectHandler {
    val activity = checkNotNull(LocalActivity.current)

    return remember(activity) {
        SubscriptionSettingsEffectHandler(activity = activity)
    }
}

internal class SubscriptionSettingsEffectHandler(
    private val activity: Activity,
) {

    fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenWirelessAlerts -> {
                try {
                    activity.startActivity(UIIntents.get().wirelessAlertsIntent)
                } catch (e: ActivityNotFoundException) {
                    LogUtil.e(LogUtil.BUGLE_TAG, "Failed to launch wireless alerts activity", e)
                }
            }
        }
    }
}
