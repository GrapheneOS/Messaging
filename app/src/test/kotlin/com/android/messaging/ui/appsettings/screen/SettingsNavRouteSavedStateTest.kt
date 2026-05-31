package com.android.messaging.ui.appsettings.screen

import androidx.compose.runtime.saveable.SaverScope
import com.android.messaging.ui.appsettings.screen.model.SettingsNavRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

internal class SettingsNavRouteSavedStateTest {

    @Test
    fun saver_roundTripsMainRoute() {
        assertRoundTrip(
            route = SettingsNavRoute.Main,
        )
    }

    @Test
    fun saver_roundTripsAppSettingsRoute() {
        assertRoundTrip(
            route = SettingsNavRoute.AppSettings,
        )
    }

    @Test
    fun saver_roundTripsSubscriptionSettingsRoute() {
        assertRoundTrip(
            route = SettingsNavRoute.SubscriptionSettings(
                subId = 5,
                title = "SIM settings",
            ),
        )
    }

    private fun assertRoundTrip(route: SettingsNavRoute) {
        val saverScope = SaverScope { true }
        val savedState = with(SettingsNavRouteSavedState.Saver) {
            with(saverScope) {
                save(route)
            }
        }

        assertNotNull(savedState)

        val restoredRoute = with(SettingsNavRouteSavedState.Saver) {
            restore(savedState!!)
        }

        assertEquals(route, restoredRoute)
    }
}
