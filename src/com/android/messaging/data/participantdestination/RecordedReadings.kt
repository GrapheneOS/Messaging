package com.android.messaging.data.participantdestination

import com.android.messaging.data.participantdestination.model.BareNumberReading
import com.android.messaging.data.participantdestination.model.RecordedReading
import com.android.messaging.data.participantdestination.model.Renormalization

internal fun withReadings(
    recordedReadings: Map<String, RecordedReading>,
    readings: List<BareNumberReading>,
    renormalizations: List<Renormalization>,
): Map<String, RecordedReading> {
    val canonicalDestinations = renormalizations.associate { renormalization ->
        renormalization.participant.destination to renormalization.canonicalDestination
    }

    return recordedReadings + readings
        .filter { reading ->
            reading.readingSubIds.isNotEmpty()
        }
        .associate { reading ->
            val destination = reading.participant.destination
            destination to RecordedReading(
                subIds = reading.readingSubIds,
                canonicalDestination = canonicalDestinations[destination]
                    ?: recordedReadings[destination]?.canonicalDestination,
            )
        }
}
