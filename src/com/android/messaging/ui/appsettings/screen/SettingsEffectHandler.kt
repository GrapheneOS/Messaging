package com.android.messaging.ui.appsettings.screen

import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.android.messaging.di.appsettings.SettingsEntryPoint
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.appsettings.screen.model.SettingsScreenEffect as Effect
import com.android.messaging.ui.license.LicenseActivity
import com.android.messaging.util.LogUtil
import dagger.hilt.android.EntryPointAccessors

@Composable
internal fun rememberSettingsEffectHandler(): SettingsEffectHandler {
    val activity = checkNotNull(LocalActivity.current)
    val context = LocalContext.current.applicationContext

    return remember(activity, context) {
        SettingsEffectHandlerImpl(
            activity = activity,
            roleManager = EntryPointAccessors
                .fromApplication(context, SettingsEntryPoint::class.java)
                .roleManager(),
        )
    }
}

internal interface SettingsEffectHandler {
    fun handle(effect: Effect)
}

internal class SettingsEffectHandlerImpl(
    private val activity: Activity,
    private val roleManager: RoleManager,
) : SettingsEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenWirelessAlerts -> {
                try {
                    activity.startActivity(UIIntents.get().wirelessAlertsIntent)
                } catch (e: ActivityNotFoundException) {
                    LogUtil.e(LogUtil.BUGLE_TAG, "Failed to launch wireless alerts activity", e)
                }
            }

            is Effect.OpenManageDefaultApps -> {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                activity.startActivity(intent)
            }

            is Effect.OpenNotificationSettings -> {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                }
                activity.startActivity(intent)
            }

            is Effect.RequestDefaultSmsApp -> {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                activity.startActivityForResult(intent, REQUEST_DEFAULT_SMS_APP)
            }

            is Effect.OpenLicenses -> {
                val intent = Intent(activity, LicenseActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }

    private companion object {
        const val REQUEST_DEFAULT_SMS_APP = 0
    }
}
