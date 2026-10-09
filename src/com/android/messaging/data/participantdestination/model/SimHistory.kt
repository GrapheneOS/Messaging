package com.android.messaging.data.participantdestination.model

internal data class SimHistory(
    val lastActiveSubIds: Set<Int>,
    val recordedReadings: Map<String, RecordedReading>,
)
