package com.android.messaging.data.satellite.model

import android.telephony.NetworkRegistrationInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SatelliteConnectionTest {

    @Test
    fun satelliteConnectionOf_returnsNullForTerrestrialRegistrations() {
        val connection = satelliteConnectionOf(
            listOf(
                registration(
                    isNonTerrestrial = false,
                    NetworkRegistrationInfo.SERVICE_TYPE_SMS,
                    NetworkRegistrationInfo.SERVICE_TYPE_MMS,
                ),
            ),
        )

        assertNull(connection)
    }

    @Test
    fun satelliteConnectionOf_reportsTheServicesOfTheSatelliteRegistrationsOnly() {
        val connection = satelliteConnectionOf(
            listOf(
                registration(
                    isNonTerrestrial = false,
                    NetworkRegistrationInfo.SERVICE_TYPE_MMS,
                ),
                registration(
                    isNonTerrestrial = true,
                    NetworkRegistrationInfo.SERVICE_TYPE_SMS,
                ),
            ),
        )

        assertEquals(
            SatelliteConnection(isSmsAvailable = true, isMmsAvailable = false),
            connection,
        )
    }

    @Test
    fun satelliteConnectionOf_combinesTheServicesOfEverySatelliteRegistration() {
        val connection = satelliteConnectionOf(
            listOf(
                registration(
                    isNonTerrestrial = true,
                    NetworkRegistrationInfo.SERVICE_TYPE_SMS,
                ),
                registration(
                    isNonTerrestrial = true,
                    NetworkRegistrationInfo.SERVICE_TYPE_DATA,
                    NetworkRegistrationInfo.SERVICE_TYPE_MMS,
                ),
            ),
        )

        assertEquals(
            SatelliteConnection(isSmsAvailable = true, isMmsAvailable = true),
            connection,
        )
    }

    @Test
    fun satelliteConnectionOf_reportsASatelliteConnectionWithoutMessagingServices() {
        val connection = satelliteConnectionOf(
            listOf(
                registration(
                    isNonTerrestrial = true,
                    NetworkRegistrationInfo.SERVICE_TYPE_DATA,
                ),
            ),
        )

        assertEquals(
            SatelliteConnection(isSmsAvailable = false, isMmsAvailable = false),
            connection,
        )
    }

    private fun registration(
        isNonTerrestrial: Boolean,
        vararg services: Int,
    ): NetworkRegistrationInfo {
        val registration = mockk<NetworkRegistrationInfo>()
        every { registration.isNonTerrestrialNetwork } returns isNonTerrestrial
        every { registration.availableServices } returns services.toList()
        return registration
    }
}
