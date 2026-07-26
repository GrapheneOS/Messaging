package com.android.messaging.ui.appsettings.general

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.provider.Settings
import com.android.messaging.ui.appsettings.general.model.AppSettingsScreenEffect
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppSettingsEffectHandlerTest {

    private val activity = mockk<Activity>(relaxed = true)
    private val roleManager = mockk<RoleManager>()

    @Before
    fun setUp() {
        every { activity.packageName } returns APP_PACKAGE_NAME
    }

    @Test
    fun handle_openManageDefaultApps_startsManageDefaultAppsSettings() {
        val startedIntent = slot<Intent>()

        createHandler().handle(AppSettingsScreenEffect.OpenManageDefaultApps)

        verify(exactly = 1) {
            activity.startActivity(capture(startedIntent))
        }
        assertEquals(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, startedIntent.captured.action)
    }

    @Test
    fun handle_openNotificationSettings_startsPackageNotificationSettings() {
        val startedIntent = slot<Intent>()

        createHandler().handle(AppSettingsScreenEffect.OpenNotificationSettings)

        verify(exactly = 1) {
            activity.startActivity(capture(startedIntent))
        }
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, startedIntent.captured.action)
        assertEquals(
            APP_PACKAGE_NAME,
            startedIntent.captured.getStringExtra(Settings.EXTRA_APP_PACKAGE),
        )
    }

    @Test
    fun handle_requestDefaultSmsApp_startsRoleRequestForResult() {
        val requestIntent = Intent(DEFAULT_SMS_ROLE_REQUEST_ACTION)
        every { roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS) } returns requestIntent

        createHandler().handle(AppSettingsScreenEffect.RequestDefaultSmsApp)

        verify(exactly = 1) {
            activity.startActivityForResult(requestIntent, REQUEST_DEFAULT_SMS_APP)
        }
    }

    private fun createHandler(): AppSettingsEffectHandler {
        return AppSettingsEffectHandler(
            activity = activity,
            roleManager = roleManager,
        )
    }

    private companion object {
        private const val APP_PACKAGE_NAME = "com.android.messaging"
        private const val DEFAULT_SMS_ROLE_REQUEST_ACTION = "request-default-sms-role"
        private const val REQUEST_DEFAULT_SMS_APP = 0
    }
}
