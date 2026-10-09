package com.android.messaging.data.participantdestination.model

internal data class RecordedReading(
    val subIds: Set<Int>,
    val canonicalDestination: String?,
)
