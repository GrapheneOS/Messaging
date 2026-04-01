package com.android.messaging.ui.appsettings.redesign.screen

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.android.messaging.ui.LicenseActivity
import com.android.messaging.ui.UIIntents
import com.android.messaging.util.LogUtil
import com.android.messaging.ui.appsettings.redesign.screen.model.SettingsScreenEffect as Effect

internal interface SettingsEffectHandler {
    fun handle(effect: Effect)
}

internal class SettingsEffectHandlerImpl(
    private val context: Context,
) : SettingsEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenWirelessAlerts -> {
                try {
                    context.startActivity(UIIntents.get().wirelessAlertsIntent)
                } catch (e: ActivityNotFoundException) {
                    LogUtil.e(LogUtil.BUGLE_TAG, "Failed to launch wireless alerts activity", e)
                }
            }

            is Effect.OpenManageDefaultApps -> {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }

            is Effect.OpenNotificationSettings -> {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            }

            is Effect.RequestDefaultSmsApp -> {
                val roleManager = context.getSystemService(RoleManager::class.java)
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                context.startActivity(intent)
            }

            is Effect.OpenLicenses -> {
                val intent = Intent(context, LicenseActivity::class.java)
                context.startActivity(intent)
            }
        }
    }
}
