package com.android.messaging.data.satellite.model

import android.telephony.NetworkRegistrationInfo
import android.telephony.ServiceState
import androidx.compose.runtime.Immutable

/**
 * What the carrier offers over the satellite network a subscription is registered on.
 *
 * The platform marks a [NetworkRegistrationInfo] as non-terrestrial and fills its available
 * services from the carrier's satellite configuration for that network.
 */
@Immutable
internal data class SatelliteConnection(
    val isSmsAvailable: Boolean,
    val isMmsAvailable: Boolean,
)

/**
 * Returns the satellite connection described by [registrations], or `null` when none of them is
 * a non-terrestrial network.
 */
internal fun satelliteConnectionOf(
    registrations: List<NetworkRegistrationInfo>,
): SatelliteConnection? {
    val satelliteRegistrations = registrations.filter { registration ->
        registration.isNonTerrestrialNetwork
    }
    if (satelliteRegistrations.isEmpty()) {
        return null
    }

    val services = satelliteRegistrations
        .flatMapTo(mutableSetOf()) { registration -> registration.availableServices }

    return SatelliteConnection(
        isSmsAvailable = NetworkRegistrationInfo.SERVICE_TYPE_SMS in services,
        isMmsAvailable = NetworkRegistrationInfo.SERVICE_TYPE_MMS in services,
    )
}

internal fun satelliteConnectionOf(serviceState: ServiceState): SatelliteConnection? {
    return satelliteConnectionOf(serviceState.networkRegistrationInfoList)
}
