package com.android.messaging.ui.appsettings.screen.effecthandler

import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.appsettings.screen.SettingsEffectHandlerImpl
import com.android.messaging.ui.appsettings.screen.model.SettingsScreenEffect
import com.android.messaging.ui.license.LicenseActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsEffectHandlerImplTest {

    private val activity = mockk<Activity>(relaxed = true)
    private val roleManager = mockk<RoleManager>()
    private val uiIntents = mockk<UIIntents>()

    @Before
    fun setUp() {
        mockkStatic(UIIntents::class)
        every { UIIntents.get() } returns uiIntents
        every { activity.packageName } returns APP_PACKAGE_NAME
    }

    @After
    fun tearDown() {
        unmockkStatic(UIIntents::class)
    }

    @Test
    fun handle_openWirelessAlerts_startsWirelessAlertsIntent() {
        val wirelessAlertsIntent = Intent(WIRELESS_ALERTS_ACTION)
        every { uiIntents.getWirelessAlertsIntent() } returns wirelessAlertsIntent
        val handler = createHandler()

        handler.handle(SettingsScreenEffect.OpenWirelessAlerts(subId = 1))

        verify(exactly = 1) {
            activity.startActivity(wirelessAlertsIntent)
        }
    }

    @Test
    fun handle_openWirelessAlerts_whenActivityIsMissing_doesNotThrow() {
        val wirelessAlertsIntent = Intent(WIRELESS_ALERTS_ACTION)
        every { uiIntents.getWirelessAlertsIntent() } returns wirelessAlertsIntent
        every {
            activity.startActivity(wirelessAlertsIntent)
        } throws ActivityNotFoundException()
        val handler = createHandler()

        handler.handle(SettingsScreenEffect.OpenWirelessAlerts(subId = 1))

        verify(exactly = 1) {
            activity.startActivity(wirelessAlertsIntent)
        }
    }

    @Test
    fun handle_openManageDefaultApps_startsManageDefaultAppsSettings() {
        val startedIntent = slot<Intent>()
        val handler = createHandler()

        handler.handle(SettingsScreenEffect.OpenManageDefaultApps)

        verify(exactly = 1) {
            activity.startActivity(capture(startedIntent))
        }
        assertEquals(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, startedIntent.captured.action)
    }

    @Test
    fun handle_openNotificationSettings_startsPackageNotificationSettings() {
        val startedIntent = slot<Intent>()
        val handler = createHandler()

        handler.handle(SettingsScreenEffect.OpenNotificationSettings)

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
        every {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
        } returns requestIntent
        val handler = createHandler()

        handler.handle(SettingsScreenEffect.RequestDefaultSmsApp)

        verify(exactly = 1) {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
        }
        verify(exactly = 1) {
            activity.startActivityForResult(requestIntent, REQUEST_DEFAULT_SMS_APP)
        }
    }

    @Test
    fun handle_openLicenses_startsLicenseActivity() {
        val startedIntent = slot<Intent>()
        val handler = createHandler()

        handler.handle(SettingsScreenEffect.OpenLicenses)

        verify(exactly = 1) {
            activity.startActivity(capture(startedIntent))
        }
        assertEquals(LicenseActivity::class.java.name, startedIntent.captured.component?.className)
        assertEquals(APP_PACKAGE_NAME, startedIntent.captured.component?.packageName)
    }

    private fun createHandler(): SettingsEffectHandlerImpl {
        return SettingsEffectHandlerImpl(
            activity = activity,
            roleManager = roleManager,
        )
    }

    private companion object {
        private const val APP_PACKAGE_NAME = "com.android.messaging"
        private const val DEFAULT_SMS_ROLE_REQUEST_ACTION = "request-default-sms-role"
        private const val REQUEST_DEFAULT_SMS_APP = 0
        private const val WIRELESS_ALERTS_ACTION = "wireless-alerts"
    }
}
