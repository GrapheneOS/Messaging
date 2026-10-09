package com.android.messaging.data.participantdestination

import com.android.messaging.data.participantdestination.model.BareNumberParticipant
import com.android.messaging.data.participantdestination.model.BareNumberReading
import com.android.messaging.data.participantdestination.model.RecordedReading
import com.android.messaging.data.participantdestination.model.Renormalization
import com.android.messaging.data.participantdestination.model.SimHistory
import com.android.messaging.data.participantdestination.store.ParticipantDestinationDatabaseStore
import com.android.messaging.data.participantdestination.store.ParticipantDestinationPreferencesStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParticipantDestinationNormalizerImplTest {

    private val callsOutsideTheLock = mutableListOf<String>()

    private val preferencesStore = FakeParticipantDestinationPreferencesStore()
    private val databaseStore = FakeParticipantDestinationDatabaseStore()
    private val bareNumberReader = FakeBareNumberReader()

    private val normalizer = ParticipantDestinationNormalizerImpl(
        preferencesStore = preferencesStore,
        databaseStore = databaseStore,
        bareNumberReader = bareNumberReader,
        databaseVersion = DATABASE_VERSION,
    )

    @Test
    fun aFirstRun_foldsEveryBareNumberIntoTheNumberItsSimsReadItAs() {
        databaseStore.lastParticipantId = 7L
        databaseStore.senderDestinations = setOf(CANONICAL_DESTINATION)
        bareNumberReader.readings = listOf(BARE_READING)

        assertTrue(normalizer.renormalizeIfPending())

        assertEquals(
            listOf(
                Renormalization(
                    participant = BARE_PARTICIPANT,
                    canonicalDestination = CANONICAL_DESTINATION,
                ),
            ),
            databaseStore.appliedRenormalizations,
        )
    }

    @Test
    fun aRun_savesWhatItRanWithTogetherWithTheReadingsItTook() {
        databaseStore.lastParticipantId = 7L
        databaseStore.senderDestinations = setOf(CANONICAL_DESTINATION)
        bareNumberReader.readings = listOf(BARE_READING)

        normalizer.renormalizeIfPending()

        assertEquals(
            SavedNormalization(
                subIds = setOf(US_SUB_ID),
                databaseVersion = DATABASE_VERSION,
                lastParticipantId = 7L,
                history = SimHistory(
                    lastActiveSubIds = setOf(US_SUB_ID),
                    recordedReadings = mapOf(
                        BARE_DESTINATION to RecordedReading(
                            subIds = setOf(US_SUB_ID),
                            canonicalDestination = CANONICAL_DESTINATION,
                        ),
                    ),
                ),
            ),
            preferencesStore.savedNormalization,
        )
    }

    @Test
    fun theSimsTheLastRunHad_renormalizeNothing() {
        preferencesStore.normalizedSubIds = setOf(US_SUB_ID, FO_SUB_ID)
        bareNumberReader.activeSubIds = setOf(FO_SUB_ID)
        bareNumberReader.readings = listOf(BARE_READING)

        assertFalse(normalizer.renormalizeIfPending())

        assertNull(databaseStore.appliedRenormalizations)
        assertNull(preferencesStore.savedNormalization)
    }

    @Test
    fun noSimLoaded_renormalizesNothing_andKeepsTheSimsTheLastSyncSaw() {
        preferencesStore.history = SimHistory(
            lastActiveSubIds = setOf(FO_SUB_ID),
            recordedReadings = emptyMap(),
        )
        bareNumberReader.activeSubIds = emptySet()
        bareNumberReader.readings = listOf(BARE_READING.copy(readingSubIds = setOf(FO_SUB_ID)))
        databaseStore.senderDestinations = setOf(BARE_DESTINATION)

        assertFalse(normalizer.renormalizeIfPending())

        assertNull(databaseStore.appliedRenormalizations)
        assertEquals(
            SimHistory(
                lastActiveSubIds = setOf(FO_SUB_ID),
                recordedReadings = mapOf(
                    BARE_DESTINATION to RecordedReading(
                        subIds = setOf(FO_SUB_ID),
                        canonicalDestination = null,
                    ),
                ),
            ),
            preferencesStore.history,
        )
    }

    @Test
    fun aSyncBetweenRuns_recordsTheSimsInThePhoneAndTheReadings() {
        preferencesStore.normalizedSubIds = setOf(US_SUB_ID)
        preferencesStore.history = SimHistory(
            lastActiveSubIds = setOf(FO_SUB_ID),
            recordedReadings = emptyMap(),
        )
        bareNumberReader.readings = listOf(BARE_READING)
        databaseStore.senderDestinations = setOf(BARE_DESTINATION)

        normalizer.renormalizeIfPending()

        assertEquals(
            SimHistory(
                lastActiveSubIds = setOf(US_SUB_ID),
                recordedReadings = mapOf(
                    BARE_DESTINATION to RecordedReading(
                        subIds = setOf(US_SUB_ID),
                        canonicalDestination = null,
                    ),
                ),
            ),
            preferencesStore.history,
        )
    }

    @Test
    fun anUnchangedHistory_isNotSavedAgain() {
        preferencesStore.normalizedSubIds = setOf(US_SUB_ID)
        preferencesStore.history = SimHistory(
            lastActiveSubIds = setOf(US_SUB_ID),
            recordedReadings = mapOf(
                BARE_DESTINATION to RecordedReading(
                    subIds = setOf(US_SUB_ID),
                    canonicalDestination = null,
                ),
            ),
        )
        bareNumberReader.readings = listOf(BARE_READING)
        databaseStore.senderDestinations = setOf(BARE_DESTINATION)

        normalizer.renormalizeIfPending()

        assertEquals(0, preferencesStore.simHistorySaveCount)
    }

    @Test
    fun readingsOfNumbersNoParticipantHolds_areForgotten() {
        preferencesStore.normalizedSubIds = setOf(US_SUB_ID)
        preferencesStore.history = SimHistory(
            lastActiveSubIds = setOf(US_SUB_ID),
            recordedReadings = mapOf(
                "5550100" to RecordedReading(
                    subIds = setOf(US_SUB_ID),
                    canonicalDestination = null,
                ),
                "211234" to RecordedReading(
                    subIds = setOf(FO_SUB_ID),
                    canonicalDestination = "+298211234",
                ),
                "5550199" to RecordedReading(
                    subIds = setOf(US_SUB_ID),
                    canonicalDestination = null,
                ),
            ),
        )
        databaseStore.senderDestinations = setOf("5550100", "+298211234")

        normalizer.renormalizeIfPending()

        assertEquals(setOf("5550100", "211234"), preferencesStore.history.recordedReadings.keys)
    }

    @Test
    fun noReadings_leaveTheSendersUnread() {
        preferencesStore.normalizedSubIds = setOf(US_SUB_ID)

        normalizer.renormalizeIfPending()

        assertEquals(0, databaseStore.senderDestinationReadCount)
    }

    @Test
    fun aRetry_renormalizesOnlyTheParticipantsTheLastFullRunHad() {
        preferencesStore.normalizedSubIds = setOf(FO_SUB_ID)
        preferencesStore.normalizedParticipantId = 5L
        databaseStore.lastParticipantId = 9L
        bareNumberReader.activeSubIds = setOf(US_SUB_ID, FO_SUB_ID)
        bareNumberReader.readings = listOf(
            readingOf(participantId = "5", destination = "5550100"),
            readingOf(participantId = "6", destination = "5550101"),
        )
        bareNumberReader.canonicalDestinations = mapOf(
            "5550100" to "+15550100",
            "5550101" to "+15550101",
        )

        assertTrue(normalizer.renormalizeIfPending())

        assertEquals(
            listOf("5"),
            databaseStore.appliedRenormalizations?.map { it.participant.participantId },
        )
        assertEquals(5L, preferencesStore.savedNormalization?.lastParticipantId)
    }

    @Test
    fun aFullRun_renormalizesUpToTheLastParticipantTheTablesHadBeforeReading() {
        databaseStore.lastParticipantId = 6L
        bareNumberReader.readings = listOf(
            readingOf(participantId = "6", destination = "5550100"),
            readingOf(participantId = "7", destination = "5550101"),
        )
        bareNumberReader.canonicalDestinations = mapOf(
            "5550100" to "+15550100",
            "5550101" to "+15550101",
        )

        normalizer.renormalizeIfPending()

        assertEquals(
            listOf("6"),
            databaseStore.appliedRenormalizations?.map { it.participant.participantId },
        )
    }

    @Test
    fun aNumberItsSimsCannotRead_isLeftAlone() {
        databaseStore.lastParticipantId = 7L
        bareNumberReader.readings = listOf(BARE_READING)
        bareNumberReader.canonicalDestinations = emptyMap()

        assertFalse(normalizer.renormalizeIfPending())

        assertEquals(emptyList<Renormalization>(), databaseStore.appliedRenormalizations)
        assertEquals(setOf(US_SUB_ID), preferencesStore.savedNormalization?.subIds)
    }

    @Test
    fun aRun_readsAndSavesHoldingTheLock() {
        databaseStore.lastParticipantId = 7L
        databaseStore.senderDestinations = setOf(CANONICAL_DESTINATION)
        bareNumberReader.readings = listOf(BARE_READING)

        normalizer.renormalizeIfPending()

        assertEquals(1, preferencesStore.lockCount)
        assertEquals(emptyList<String>(), callsOutsideTheLock)
    }

    @Test
    fun aSyncBetweenRuns_readsAndSavesHoldingTheLock() {
        preferencesStore.normalizedSubIds = setOf(US_SUB_ID)
        bareNumberReader.readings = listOf(BARE_READING)
        databaseStore.senderDestinations = setOf(BARE_DESTINATION)

        normalizer.renormalizeIfPending()

        assertEquals(1, preferencesStore.simHistorySaveCount)
        assertEquals(1, preferencesStore.lockCount)
        assertEquals(emptyList<String>(), callsOutsideTheLock)
    }

    @Test
    fun aNewDatabase_schedulesAFullRunWithoutTakingTheLock() {
        preferencesStore.normalizedSubIds = setOf(US_SUB_ID)

        normalizer.onDatabaseCreated()

        assertEquals(1, preferencesStore.clearCount)
        assertEquals(0, preferencesStore.lockCount)
    }

    @Test
    fun anUpgradeFromOlderThanTheLastRun_schedulesAFullRunWithoutTakingTheLock() {
        preferencesStore.normalizedDatabaseVersion = DATABASE_VERSION

        normalizer.onDatabaseUpgraded(oldVersion = DATABASE_VERSION - 1)

        assertEquals(1, preferencesStore.clearCount)
        assertEquals(0, preferencesStore.lockCount)
    }

    @Test
    fun anUpgradeFromTheLastRunsVersion_schedulesNothing() {
        preferencesStore.normalizedDatabaseVersion = DATABASE_VERSION

        normalizer.onDatabaseUpgraded(oldVersion = DATABASE_VERSION)

        assertEquals(0, preferencesStore.clearCount)
    }

    private fun readingOf(participantId: String, destination: String): BareNumberReading {
        return BareNumberReading(
            participant = BareNumberParticipant(
                participantId = participantId,
                destination = destination,
                isBlocked = false,
            ),
            readingSubIds = setOf(US_SUB_ID),
        )
    }

    private data class SavedNormalization(
        val subIds: Set<Int>,
        val databaseVersion: Int,
        val lastParticipantId: Long,
        val history: SimHistory,
    )

    private fun recordCall(name: String) {
        if (!preferencesStore.isLocked) {
            callsOutsideTheLock += name
        }
    }

    private inner class FakeParticipantDestinationPreferencesStore :
        ParticipantDestinationPreferencesStore {

        var isLocked = false
        var lockCount = 0
        var clearCount = 0
        var simHistorySaveCount = 0

        var history = SimHistory(lastActiveSubIds = emptySet(), recordedReadings = emptyMap())
        var normalizedSubIds = emptySet<Int>()
        var normalizedParticipantId = 0L
        var normalizedDatabaseVersion = 0
        var savedNormalization: SavedNormalization? = null

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
            recordCall(name = "readNormalizedSubIds")
            return normalizedSubIds
        }

        override fun readNormalizedParticipantId(): Long {
            recordCall(name = "readNormalizedParticipantId")
            return normalizedParticipantId
        }

        override fun readNormalizedDatabaseVersion(): Int {
            return normalizedDatabaseVersion
        }

        override fun saveNormalization(
            subIds: Set<Int>,
            databaseVersion: Int,
            lastParticipantId: Long,
            history: SimHistory,
        ) {
            recordCall(name = "saveNormalization")
            savedNormalization = SavedNormalization(
                subIds = subIds,
                databaseVersion = databaseVersion,
                lastParticipantId = lastParticipantId,
                history = history,
            )
            normalizedSubIds = subIds
            normalizedDatabaseVersion = databaseVersion
            normalizedParticipantId = lastParticipantId
            this.history = history
        }

        override fun clearNormalization() {
            clearCount++
            normalizedSubIds = emptySet()
            normalizedDatabaseVersion = 0
            normalizedParticipantId = 0L
        }
    }

    private inner class FakeParticipantDestinationDatabaseStore :
        ParticipantDestinationDatabaseStore {

        var lastParticipantId = 0L
        var senderDestinations = emptySet<String>()
        var senderDestinationReadCount = 0
        var appliedRenormalizations: List<Renormalization>? = null

        override fun readBareNumberParticipants(): List<BareNumberParticipant> {
            error("The normalizer reads the participants through the reader")
        }

        override fun readSubIdsByParticipant(): Map<String, Set<Int>> {
            error("The normalizer reads the participants through the reader")
        }

        override fun readSenderDestinations(): Set<String> {
            recordCall(name = "readSenderDestinations")
            senderDestinationReadCount++
            return senderDestinations
        }

        override fun readLastParticipantId(): Long {
            recordCall(name = "readLastParticipantId")
            return lastParticipantId
        }

        override fun applyRenormalizations(renormalizations: List<Renormalization>) {
            recordCall(name = "applyRenormalizations")
            appliedRenormalizations = renormalizations
        }
    }

    private inner class FakeBareNumberReader : BareNumberReader {

        var activeSubIds = setOf(US_SUB_ID)
        var readings = emptyList<BareNumberReading>()
        var canonicalDestinations = mapOf(BARE_DESTINATION to CANONICAL_DESTINATION)

        override fun readActiveSubIds(): Set<Int> {
            recordCall(name = "readActiveSubIds")
            return activeSubIds
        }

        override fun readBareNumbers(
            activeSubIds: Set<Int>,
            history: SimHistory,
        ): List<BareNumberReading> {
            recordCall(name = "readBareNumbers")
            return readings
        }

        override fun readBareNumbersAmong(
            destinations: Set<String>,
            activeSubIds: Set<Int>,
            history: SimHistory,
        ): List<BareNumberReading> {
            error("Only blocking reads the numbers among some")
        }

        override fun canonicalDestinationOf(
            reading: BareNumberReading,
            activeSubIds: Set<Int>,
        ): String? {
            recordCall(name = "canonicalDestinationOf")
            return canonicalDestinations[reading.participant.destination]
        }
    }

    private companion object {
        private const val DATABASE_VERSION = 5
        private const val US_SUB_ID = 1
        private const val FO_SUB_ID = 2
        private const val BARE_DESTINATION = "211234"
        private const val CANONICAL_DESTINATION = "+298211234"

        private val BARE_PARTICIPANT = BareNumberParticipant(
            participantId = "3",
            destination = BARE_DESTINATION,
            isBlocked = false,
        )
        private val BARE_READING = BareNumberReading(
            participant = BARE_PARTICIPANT,
            readingSubIds = setOf(US_SUB_ID),
        )
    }
}
