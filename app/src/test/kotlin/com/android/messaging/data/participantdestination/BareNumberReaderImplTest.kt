package com.android.messaging.data.participantdestination

import android.telephony.SubscriptionInfo
import com.android.messaging.data.participantdestination.model.BareNumberParticipant
import com.android.messaging.data.participantdestination.model.BareNumberReading
import com.android.messaging.data.participantdestination.model.RecordedReading
import com.android.messaging.data.participantdestination.model.Renormalization
import com.android.messaging.data.participantdestination.model.SimHistory
import com.android.messaging.data.participantdestination.store.ParticipantDestinationDatabaseStore
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSubscriptionManager

@RunWith(RobolectricTestRunner::class)
class BareNumberReaderImplTest {

    private val databaseStore = FakeParticipantDestinationDatabaseStore()
    private val defaultPhoneUtils = mockk<PhoneUtils>()
    private val canonicalDestinationsBySubId = mutableMapOf<Int, String>()

    private val bareNumberReader = BareNumberReaderImpl(databaseStore = databaseStore)

    @Before
    fun setUp() {
        mockkStatic(PhoneUtils::class)
        every { PhoneUtils.getDefault() } returns defaultPhoneUtils
        every { PhoneUtils.get(any()) } answers {
            val subId = firstArg<Int>()
            mockk<PhoneUtils> {
                every { getCanonicalBySimLocale(any()) } answers {
                    canonicalDestinationsBySubId[subId] ?: firstArg()
                }
            }
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun theSimsInThePhone_areItsActiveSubscriptions() {
        every { defaultPhoneUtils.activeSubscriptionInfoList } returns listOf(
            subscriptionInfoOf(subId = US_SUB_ID),
            subscriptionInfoOf(subId = FO_SUB_ID),
        )

        assertEquals(setOf(US_SUB_ID, FO_SUB_ID), bareNumberReader.readActiveSubIds())
    }

    @Test
    fun aNumberWhoseMessagesWentThroughKnownSims_isReadWithThoseAlone() {
        databaseStore.subIdsByParticipant = mapOf(PARTICIPANT_ID to setOf(US_SUB_ID))

        assertEquals(setOf(US_SUB_ID), readingSubIdsOfTheBareNumber())
    }

    @Test
    fun aNumberWithAMessageThroughAnUnknownSim_isReadWithTheSimsInThePhoneNowAndAtTheLastSync() {
        databaseStore.subIdsByParticipant = mapOf(
            PARTICIPANT_ID to setOf(US_SUB_ID, ParticipantData.DEFAULT_SELF_SUB_ID),
        )

        assertEquals(setOf(US_SUB_ID, FO_SUB_ID, GL_SUB_ID), readingSubIdsOfTheBareNumber())
    }

    @Test
    fun aNumberWithNoMessage_isReadWithTheSimsInThePhoneNowAndAtTheLastSync() {
        databaseStore.subIdsByParticipant = emptyMap()

        assertEquals(setOf(FO_SUB_ID, GL_SUB_ID), readingSubIdsOfTheBareNumber())
    }

    @Test
    fun aNumberReadBefore_isReadWithTheSimsItWasReadWithThenToo() {
        databaseStore.subIdsByParticipant = mapOf(PARTICIPANT_ID to setOf(US_SUB_ID))

        assertEquals(
            setOf(US_SUB_ID, GL_SUB_ID),
            readingSubIdsOfTheBareNumber(
                recordedReadings = mapOf(
                    BARE_DESTINATION to RecordedReading(
                        subIds = setOf(GL_SUB_ID),
                        canonicalDestination = null,
                    ),
                ),
            ),
        )
    }

    @Test
    fun noBareNumber_leavesTheMessagesUnscanned() {
        databaseStore.participants = emptyList()

        val readings = bareNumberReader.readBareNumbers(
            activeSubIds = setOf(FO_SUB_ID),
            history = HISTORY,
        )

        assertEquals(emptyList<BareNumberReading>(), readings)
        assertEquals(0, databaseStore.subIdReadCount)
    }

    @Test
    fun aNumberEverySimReadsAlike_isReadAsThatNumber() {
        canonicalDestinationsBySubId[US_SUB_ID] = CANONICAL_DESTINATION
        canonicalDestinationsBySubId[FO_SUB_ID] = CANONICAL_DESTINATION

        assertEquals(
            CANONICAL_DESTINATION,
            canonicalDestinationOfTheBareNumber(
                readingSubIds = setOf(US_SUB_ID, FO_SUB_ID),
                activeSubIds = setOf(US_SUB_ID, FO_SUB_ID),
            ),
        )
    }

    @Test
    fun aNumberTheSimsReadDifferently_isLeftAsItIs() {
        canonicalDestinationsBySubId[FO_SUB_ID] = CANONICAL_DESTINATION

        assertNull(
            canonicalDestinationOfTheBareNumber(
                readingSubIds = setOf(US_SUB_ID, FO_SUB_ID),
                activeSubIds = setOf(US_SUB_ID, FO_SUB_ID),
            ),
        )
    }

    @Test
    fun aNumberTheSimsReadAsDifferentNumbers_isLeftAsItIs() {
        canonicalDestinationsBySubId[US_SUB_ID] = "+1211234"
        canonicalDestinationsBySubId[FO_SUB_ID] = CANONICAL_DESTINATION

        assertNull(
            canonicalDestinationOfTheBareNumber(
                readingSubIds = setOf(US_SUB_ID, FO_SUB_ID),
                activeSubIds = setOf(US_SUB_ID, FO_SUB_ID),
            ),
        )
    }

    @Test
    fun aNumberReadWithASimNoLongerInThePhone_isLeftAsItIs() {
        canonicalDestinationsBySubId[US_SUB_ID] = CANONICAL_DESTINATION
        canonicalDestinationsBySubId[GL_SUB_ID] = CANONICAL_DESTINATION

        assertNull(
            canonicalDestinationOfTheBareNumber(
                readingSubIds = setOf(US_SUB_ID, GL_SUB_ID),
                activeSubIds = setOf(US_SUB_ID),
            ),
        )
    }

    @Test
    fun aNumberEverySimLeavesAsItIs_hasNothingToBeReadAs() {
        assertNull(
            canonicalDestinationOfTheBareNumber(
                readingSubIds = setOf(US_SUB_ID),
                activeSubIds = setOf(US_SUB_ID),
            ),
        )
    }

    private fun readingSubIdsOfTheBareNumber(
        recordedReadings: Map<String, RecordedReading> = emptyMap(),
    ): Set<Int>? {
        return bareNumberReader.readBareNumbers(
            activeSubIds = setOf(FO_SUB_ID),
            history = HISTORY.copy(recordedReadings = recordedReadings),
        ).singleOrNull()?.readingSubIds
    }

    private fun canonicalDestinationOfTheBareNumber(
        readingSubIds: Set<Int>,
        activeSubIds: Set<Int>,
    ): String? {
        return bareNumberReader.canonicalDestinationOf(
            reading = BareNumberReading(
                participant = BARE_PARTICIPANT,
                readingSubIds = readingSubIds,
            ),
            activeSubIds = activeSubIds,
        )
    }

    private fun subscriptionInfoOf(subId: Int): SubscriptionInfo {
        return ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder()
            .setId(subId)
            .buildSubscriptionInfo()
    }

    private class FakeParticipantDestinationDatabaseStore : ParticipantDestinationDatabaseStore {

        var participants = listOf(BARE_PARTICIPANT)
        var subIdsByParticipant = emptyMap<String, Set<Int>>()
        var subIdReadCount = 0

        override fun readBareNumberParticipants(): List<BareNumberParticipant> {
            return participants
        }

        override fun readSubIdsByParticipant(): Map<String, Set<Int>> {
            subIdReadCount++
            return subIdsByParticipant
        }

        override fun readSenderDestinations(): Set<String> {
            error("The reader reads the bare numbers alone")
        }

        override fun readLastParticipantId(): Long {
            error("The reader reads the bare numbers alone")
        }

        override fun applyRenormalizations(renormalizations: List<Renormalization>) {
            error("The reader changes nothing")
        }
    }

    private companion object {
        private const val US_SUB_ID = 1
        private const val FO_SUB_ID = 2
        private const val GL_SUB_ID = 3
        private const val PARTICIPANT_ID = "3"
        private const val BARE_DESTINATION = "211234"
        private const val CANONICAL_DESTINATION = "+298211234"

        private val BARE_PARTICIPANT = BareNumberParticipant(
            participantId = PARTICIPANT_ID,
            destination = BARE_DESTINATION,
            isBlocked = false,
        )

        private val HISTORY = SimHistory(
            lastActiveSubIds = setOf(GL_SUB_ID),
            recordedReadings = emptyMap(),
        )
    }
}
