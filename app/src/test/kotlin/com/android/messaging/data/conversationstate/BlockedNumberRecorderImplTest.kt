package com.android.messaging.data.conversationstate

import com.android.messaging.data.participantdestination.BareNumberReader
import com.android.messaging.data.participantdestination.model.BareNumberParticipant
import com.android.messaging.data.participantdestination.model.BareNumberReading
import com.android.messaging.data.participantdestination.model.RecordedReading
import com.android.messaging.data.participantdestination.model.SimHistory
import com.android.messaging.data.participantdestination.store.ParticipantDestinationPreferencesStore
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockedNumberRecorderImplTest {

    private val callsOutsideTheLock = mutableListOf<String>()

    private val preferencesStore = FakeParticipantDestinationPreferencesStore()
    private val bareNumberReader = FakeBareNumberReader()

    private val recorder = BlockedNumberRecorderImpl(
        preferencesStore = preferencesStore,
        bareNumberReader = bareNumberReader,
    )

    @Test
    fun aBlockedBareNumber_isRecordedWithTheSimsItCameThrough() {
        bareNumberReader.readings = listOf(BLOCKED_READING)

        recorder.record(destinations = setOf(BLOCKED_DESTINATION))

        assertEquals(
            mapOf(
                BLOCKED_DESTINATION to RecordedReading(
                    subIds = setOf(US_SUB_ID, FO_SUB_ID),
                    canonicalDestination = null,
                ),
            ),
            preferencesStore.history.recordedReadings,
        )
    }

    @Test
    fun onlyTheBlockedNumbers_areRead() {
        recorder.record(destinations = setOf(BLOCKED_DESTINATION))

        assertEquals(listOf(setOf(BLOCKED_DESTINATION)), bareNumberReader.readDestinations)
    }

    @Test
    fun theOtherReadingsAndTheLastSyncsSims_stay() {
        preferencesStore.history = SimHistory(
            lastActiveSubIds = setOf(FO_SUB_ID),
            recordedReadings = mapOf(OTHER_DESTINATION to OTHER_READING),
        )
        bareNumberReader.activeSubIds = setOf(US_SUB_ID)
        bareNumberReader.readings = listOf(BLOCKED_READING)

        recorder.record(destinations = setOf(BLOCKED_DESTINATION))

        assertEquals(setOf(FO_SUB_ID), preferencesStore.history.lastActiveSubIds)
        assertEquals(OTHER_READING, preferencesStore.history.recordedReadings[OTHER_DESTINATION])
    }

    @Test
    fun aNumberAlreadyRecordedWithTheSameSims_isNotSavedAgain() {
        preferencesStore.history = SimHistory(
            lastActiveSubIds = setOf(US_SUB_ID),
            recordedReadings = mapOf(
                BLOCKED_DESTINATION to RecordedReading(
                    subIds = setOf(US_SUB_ID, FO_SUB_ID),
                    canonicalDestination = null,
                ),
            ),
        )
        bareNumberReader.readings = listOf(BLOCKED_READING)

        recorder.record(destinations = setOf(BLOCKED_DESTINATION))

        assertEquals(0, preferencesStore.simHistorySaveCount)
    }

    @Test
    fun aBlockedNumberThatIsNotBare_recordsNothing() {
        recorder.record(destinations = setOf("+15550100"))

        assertEquals(0, preferencesStore.simHistorySaveCount)
    }

    @Test
    fun theReadings_areReadAndSavedHoldingTheLock() {
        bareNumberReader.readings = listOf(BLOCKED_READING)

        recorder.record(destinations = setOf(BLOCKED_DESTINATION))

        assertEquals(1, preferencesStore.lockCount)
        assertEquals(1, preferencesStore.simHistorySaveCount)
        assertEquals(emptyList<String>(), callsOutsideTheLock)
    }

    private fun recordCall(name: String) {
        if (!preferencesStore.isLocked) {
            callsOutsideTheLock += name
        }
    }

    private inner class FakeParticipantDestinationPreferencesStore :
        ParticipantDestinationPreferencesStore {

        var isLocked = false
        var lockCount = 0
        var simHistorySaveCount = 0
        var history = SimHistory(lastActiveSubIds = setOf(US_SUB_ID), recordedReadings = emptyMap())

        override fun <T> withLock(block: () -> T): T {
            lockCount++
            isLocked = true
            return try {
                block()
            } finally {
                isLocked = false
            }
        }

        override fun readSimHistory(): SimHistory {
            recordCall(name = "readSimHistory")
            return history
        }

        override fun saveSimHistory(history: SimHistory) {
            recordCall(name = "saveSimHistory")
            simHistorySaveCount++
            this.history = history
        }

        override fun readNormalizedSubIds(): Set<Int> {
            error("Recording a block doesn't renormalize")
        }

        override fun readNormalizedParticipantId(): Long {
            error("Recording a block doesn't renormalize")
        }

        override fun readNormalizedDatabaseVersion(): Int {
            error("Recording a block doesn't renormalize")
        }

        override fun saveNormalization(
            subIds: Set<Int>,
            databaseVersion: Int,
            lastParticipantId: Long,
            history: SimHistory,
        ) {
            error("Recording a block doesn't renormalize")
        }

        override fun clearNormalization() {
            error("Recording a block doesn't renormalize")
        }
    }

    private inner class FakeBareNumberReader : BareNumberReader {

        var activeSubIds = setOf(US_SUB_ID)
        var readings = emptyList<BareNumberReading>()
        val readDestinations = mutableListOf<Set<String>>()

        override fun readActiveSubIds(): Set<Int> {
            recordCall(name = "readActiveSubIds")
            return activeSubIds
        }

        override fun readBareNumbers(
            activeSubIds: Set<Int>,
            history: SimHistory,
        ): List<BareNumberReading> {
            error("Recording a block reads only the blocked numbers")
        }

        override fun readBareNumbersAmong(
            destinations: Set<String>,
            activeSubIds: Set<Int>,
            history: SimHistory,
        ): List<BareNumberReading> {
            recordCall(name = "readBareNumbersAmong")
            readDestinations += destinations
            return readings.filter { reading -> reading.participant.destination in destinations }
        }

        override fun canonicalDestinationOf(
            reading: BareNumberReading,
            activeSubIds: Set<Int>,
        ): String? {
            error("Recording a block doesn't renormalize")
        }
    }

    private companion object {
        private const val US_SUB_ID = 1
        private const val FO_SUB_ID = 2
        private const val BLOCKED_DESTINATION = "211234"
        private const val OTHER_DESTINATION = "5550100"

        private val BLOCKED_READING = BareNumberReading(
            participant = BareNumberParticipant(
                participantId = "3",
                destination = BLOCKED_DESTINATION,
                isBlocked = true,
            ),
            readingSubIds = setOf(US_SUB_ID, FO_SUB_ID),
        )
        private val OTHER_READING = RecordedReading(
            subIds = setOf(FO_SUB_ID),
            canonicalDestination = "+15550100",
        )
    }
}
