package com.android.messaging.data.participantdestination.model

internal data class BareNumberReading(
    val participant: BareNumberParticipant,
    val readingSubIds: Set<Int>,
)
