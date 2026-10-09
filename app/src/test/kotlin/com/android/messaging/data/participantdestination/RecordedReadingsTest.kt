package com.android.messaging.data.participantdestination

import com.android.messaging.data.participantdestination.model.BareNumberParticipant
import com.android.messaging.data.participantdestination.model.BareNumberReading
import com.android.messaging.data.participantdestination.model.RecordedReading
import com.android.messaging.data.participantdestination.model.Renormalization
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordedReadingsTest {

    @Test
    fun aReading_replacesTheOneRecordedForItsNumber() {
        val recordedReadings = withReadings(
            recordedReadings = mapOf(
                BARE_DESTINATION to RecordedReading(
                    subIds = setOf(GL_SUB_ID),
                    canonicalDestination = null,
                ),
            ),
            readings = listOf(readingOf(subIds = setOf(US_SUB_ID, FO_SUB_ID))),
            renormalizations = emptyList(),
        )

        assertEquals(
            mapOf(
                BARE_DESTINATION to RecordedReading(
                    subIds = setOf(US_SUB_ID, FO_SUB_ID),
                    canonicalDestination = null,
                ),
            ),
            recordedReadings,
        )
    }

    @Test
    fun theReadingsOfOtherNumbers_stay() {
        val recordedReadings = withReadings(
            recordedReadings = mapOf(OTHER_DESTINATION to OTHER_READING),
            readings = listOf(readingOf(subIds = setOf(US_SUB_ID))),
            renormalizations = emptyList(),
        )

        assertEquals(OTHER_READING, recordedReadings[OTHER_DESTINATION])
    }

    @Test
    fun aReadingWithNoSim_isNotRecorded() {
        val recordedReadings = withReadings(
            recordedReadings = emptyMap(),
            readings = listOf(readingOf(subIds = emptySet())),
            renormalizations = emptyList(),
        )

        assertEquals(emptyMap<String, RecordedReading>(), recordedReadings)
    }

    @Test
    fun aRenormalizedNumber_isRecordedWithTheNumberItWasNormalizedTo() {
        val recordedReadings = withReadings(
            recordedReadings = mapOf(
                BARE_DESTINATION to RecordedReading(
                    subIds = setOf(US_SUB_ID),
                    canonicalDestination = "+1211234",
                ),
            ),
            readings = listOf(readingOf(subIds = setOf(FO_SUB_ID))),
            renormalizations = listOf(
                Renormalization(
                    participant = BARE_PARTICIPANT,
                    canonicalDestination = CANONICAL_DESTINATION,
                ),
            ),
        )

        assertEquals(
            CANONICAL_DESTINATION,
            recordedReadings[BARE_DESTINATION]?.canonicalDestination,
        )
    }

    @Test
    fun aNumberNormalizedBefore_keepsTheNumberItWasNormalizedTo() {
        val recordedReadings = withReadings(
            recordedReadings = mapOf(
                BARE_DESTINATION to RecordedReading(
                    subIds = setOf(FO_SUB_ID),
                    canonicalDestination = CANONICAL_DESTINATION,
                ),
            ),
            readings = listOf(readingOf(subIds = setOf(FO_SUB_ID))),
            renormalizations = emptyList(),
        )

        assertEquals(
            CANONICAL_DESTINATION,
            recordedReadings[BARE_DESTINATION]?.canonicalDestination,
        )
    }

    private fun readingOf(subIds: Set<Int>): BareNumberReading {
        return BareNumberReading(participant = BARE_PARTICIPANT, readingSubIds = subIds)
    }

    private companion object {
        private const val US_SUB_ID = 1
        private const val FO_SUB_ID = 2
        private const val GL_SUB_ID = 3
        private const val BARE_DESTINATION = "211234"
        private const val CANONICAL_DESTINATION = "+298211234"
        private const val OTHER_DESTINATION = "5550100"

        private val BARE_PARTICIPANT = BareNumberParticipant(
            participantId = "3",
            destination = BARE_DESTINATION,
            isBlocked = false,
        )
        private val OTHER_READING = RecordedReading(
            subIds = setOf(GL_SUB_ID),
            canonicalDestination = "+15550100",
        )
    }
}
