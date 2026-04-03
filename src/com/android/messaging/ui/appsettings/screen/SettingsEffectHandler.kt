package com.android.messaging.ui.appsettings.screen

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.android.messaging.ui.LicenseActivity
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.appsettings.screen.model.SettingsScreenEffect
import com.android.messaging.util.LogUtil

internal interface SettingsEffectHandler {
    fun handle(effect: SettingsScreenEffect)
}

internal class SettingsEffectHandlerImpl(private val context: Context) : SettingsEffectHandler {

    override fun handle(effect: SettingsScreenEffect) {
        when (effect) {
            is SettingsScreenEffect.OpenWirelessAlerts -> {
                try {
                    context.startActivity(UIIntents.get().wirelessAlertsIntent)
                } catch (e: ActivityNotFoundException) {
                    LogUtil.e(LogUtil.BUGLE_TAG, "Failed to launch wireless alerts activity", e)
                }
            }

            is SettingsScreenEffect.OpenManageDefaultApps -> {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }

            is SettingsScreenEffect.OpenNotificationSettings -> {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            }

            is SettingsScreenEffect.RequestDefaultSmsApp -> {
                val roleManager = context.getSystemService(RoleManager::class.java)
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                context.startActivity(intent)
            }

            is SettingsScreenEffect.OpenLicenses -> {
                val intent = Intent(context, LicenseActivity::class.java)
                context.startActivity(intent)
            }
        }
    }
}
