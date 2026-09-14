package com.android.messaging.ui.appsettings.general

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.android.messaging.di.appsettings.SettingsEntryPoint
import com.android.messaging.ui.appsettings.general.model.AppSettingsScreenEffect as Effect
import dagger.hilt.android.EntryPointAccessors

@Composable
internal fun rememberAppSettingsEffectHandler(): AppSettingsEffectHandler {
    val activity = checkNotNull(LocalActivity.current)
    val context = LocalContext.current.applicationContext

    return remember(activity, context) {
        AppSettingsEffectHandler(
            activity = activity,
            roleManager = EntryPointAccessors
                .fromApplication(context, SettingsEntryPoint::class.java)
                .roleManager(),
        )
    }
}

internal class AppSettingsEffectHandler(
    private val activity: Activity,
    private val roleManager: RoleManager,
) {

    fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenManageDefaultApps -> {
                activity.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
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
        }
    }

    private companion object {
        const val REQUEST_DEFAULT_SMS_APP = 0
    }
}
