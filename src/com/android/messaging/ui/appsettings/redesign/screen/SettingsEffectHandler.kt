package com.android.messaging.ui.appsettings.redesign.screen

import android.content.ActivityNotFoundException
import android.content.Context
import com.android.messaging.ui.UIIntents
import com.android.messaging.util.LogUtil
import com.android.messaging.ui.appsettings.redesign.screen.model.SettingsScreenEffect as Effect

internal fun handleEffect(context: Context, effect: Effect) {
    when (effect) {
        is Effect.OpenWirelessAlerts -> {
            try {
                context.startActivity(UIIntents.get().wirelessAlertsIntent)
            } catch (e: ActivityNotFoundException) {
                LogUtil.e(LogUtil.BUGLE_TAG, "Failed to launch wireless alerts activity", e)
            }
        }
    }
}
