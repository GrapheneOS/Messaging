package com.android.messaging.ui.appsettings.subscription

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsScreenEffect
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubscriptionSettingsEffectHandlerTest {

    private val activity = mockk<Activity>(relaxed = true)
    private val uiIntents = mockk<UIIntents>()

    @Before
    fun setUp() {
        mockkStatic(UIIntents::class)
        every { UIIntents.get() } returns uiIntents
    }

    @After
    fun tearDown() {
        unmockkStatic(UIIntents::class)
    }

    @Test
    fun handle_openWirelessAlerts_startsWirelessAlertsIntent() {
        val wirelessAlertsIntent = Intent(WIRELESS_ALERTS_ACTION)
        every { uiIntents.getWirelessAlertsIntent() } returns wirelessAlertsIntent

        createHandler().handle(SubscriptionSettingsScreenEffect.OpenWirelessAlerts)

        verify(exactly = 1) {
            activity.startActivity(wirelessAlertsIntent)
        }
    }

    @Test
    fun handle_openWirelessAlerts_whenActivityIsMissing_doesNotThrow() {
        val wirelessAlertsIntent = Intent(WIRELESS_ALERTS_ACTION)
        every { uiIntents.getWirelessAlertsIntent() } returns wirelessAlertsIntent
        every { activity.startActivity(wirelessAlertsIntent) } throws ActivityNotFoundException()

        createHandler().handle(SubscriptionSettingsScreenEffect.OpenWirelessAlerts)

        verify(exactly = 1) {
            activity.startActivity(wirelessAlertsIntent)
        }
    }

    private fun createHandler(): SubscriptionSettingsEffectHandler {
        return SubscriptionSettingsEffectHandler(activity = activity)
    }

    private companion object {
        private const val WIRELESS_ALERTS_ACTION = "wireless-alerts"
    }
}
