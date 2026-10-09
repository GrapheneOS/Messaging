package com.android.messaging.data.participantdestination.store

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.android.messaging.data.participantdestination.model.RecordedReading
import com.android.messaging.data.participantdestination.model.SimHistory
import com.android.messaging.testutil.backupRulesExcluding
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ParticipantDestinationPreferencesStoreImplTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext

    private val preferences: SharedPreferences
        get() {
            return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        }

    @Test
    fun withNothingSaved_readsNoRenormalizationAndNoHistory() {
        val store = ParticipantDestinationPreferencesStoreImpl(context = context)

        assertEquals(
            SimHistory(lastActiveSubIds = emptySet(), recordedReadings = emptyMap()),
            store.readSimHistory(),
        )
        assertEquals(emptySet<Int>(), store.readNormalizedSubIds())
        assertEquals(0L, store.readNormalizedParticipantId())
        assertEquals(0, store.readNormalizedDatabaseVersion())
    }

    @Test
    fun aSavedHistory_isReadBack() {
        ParticipantDestinationPreferencesStoreImpl(context = context).saveSimHistory(
            history = SIM_HISTORY,
        )

        assertEquals(
            SIM_HISTORY,
            ParticipantDestinationPreferencesStoreImpl(context = context).readSimHistory(),
        )
    }

    @Test
    fun aReading_isSavedAsItsSimsItsCanonicalNumberAndItsNumber() {
        ParticipantDestinationPreferencesStoreImpl(context = context).saveSimHistory(
            history = SIM_HISTORY,
        )

        assertEquals(
            setOf("2,3:+298211234:211234", "4::56789"),
            preferences.getStringSet(READINGS_KEY, null),
        )
        assertEquals(setOf("2", "3"), preferences.getStringSet(LAST_ACTIVE_SUB_IDS_KEY, null))
    }

    @Test
    fun malformedReadingsAndSims_areDropped() {
        preferences.edit(commit = true) {
            putStringSet(
                READINGS_KEY,
                setOf("2,x:+298211234:211234", "3:56789", "", "4::5550100"),
            )
            putStringSet(LAST_ACTIVE_SUB_IDS_KEY, setOf("2", "x"))
        }

        assertEquals(
            SimHistory(
                lastActiveSubIds = setOf(2),
                recordedReadings = mapOf(
                    "211234" to RecordedReading(
                        subIds = setOf(2),
                        canonicalDestination = "+298211234",
                    ),
                    "5550100" to RecordedReading(subIds = setOf(4), canonicalDestination = null),
                ),
            ),
            ParticipantDestinationPreferencesStoreImpl(context = context).readSimHistory(),
        )
    }

    @Test
    fun aRenormalization_isSavedTogetherWithTheHistoryItLeft() {
        ParticipantDestinationPreferencesStoreImpl(context = context).saveNormalization(
            subIds = setOf(2, 3),
            databaseVersion = DATABASE_VERSION,
            lastParticipantId = LAST_PARTICIPANT_ID,
            history = SIM_HISTORY,
        )

        val store = ParticipantDestinationPreferencesStoreImpl(context = context)
        assertEquals(setOf(2, 3), store.readNormalizedSubIds())
        assertEquals(DATABASE_VERSION, store.readNormalizedDatabaseVersion())
        assertEquals(LAST_PARTICIPANT_ID, store.readNormalizedParticipantId())
        assertEquals(SIM_HISTORY, store.readSimHistory())
    }

    @Test
    fun clearingTheRenormalization_keepsTheHistory() {
        ParticipantDestinationPreferencesStoreImpl(context = context).saveNormalization(
            subIds = setOf(2, 3),
            databaseVersion = DATABASE_VERSION,
            lastParticipantId = LAST_PARTICIPANT_ID,
            history = SIM_HISTORY,
        )

        ParticipantDestinationPreferencesStoreImpl(context = context).clearNormalization()

        val store = ParticipantDestinationPreferencesStoreImpl(context = context)
        assertEquals(emptySet<Int>(), store.readNormalizedSubIds())
        assertEquals(0, store.readNormalizedDatabaseVersion())
        assertEquals(0L, store.readNormalizedParticipantId())
        assertEquals(SIM_HISTORY, store.readSimHistory())
        assertEquals(setOf(READINGS_KEY, LAST_ACTIVE_SUB_IDS_KEY), preferences.all.keys)
    }

    @Test
    fun theRenormalization_isKeptInTheFileExcludedFromBackups() {
        ParticipantDestinationPreferencesStoreImpl(context = context).saveNormalization(
            subIds = setOf(2, 3),
            databaseVersion = DATABASE_VERSION,
            lastParticipantId = LAST_PARTICIPANT_ID,
            history = SIM_HISTORY,
        )

        assertEquals(
            setOf(
                "normalized_sub_ids",
                "normalized_database_version",
                "normalized_participant_id",
                READINGS_KEY,
                LAST_ACTIVE_SUB_IDS_KEY,
            ),
            preferences.all.keys,
        )
        assertEquals(
            setOf("cloud-backup", "device-transfer"),
            backupRulesExcluding(context = context, sharedPreferencesName = PREFERENCES_NAME),
        )
    }

    @Test
    fun theLock_returnsWhatItsBlockReturns() {
        val store = ParticipantDestinationPreferencesStoreImpl(context = context)

        assertEquals("read", store.withLock { "read" })
    }

    @Test
    fun theLock_holdsBackASecondHolderUntilTheFirstReleasesIt() {
        val store = ParticipantDestinationPreferencesStoreImpl(context = context)
        val isFirstHolding = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val firstHolder = thread {
            store.withLock {
                isFirstHolding.countDown()
                releaseFirst.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            }
        }
        assertTrue(isFirstHolding.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))

        val hasSecondRun = AtomicBoolean(false)
        val secondHolder = thread { store.withLock { hasSecondRun.set(true) } }
        secondHolder.awaitFinishedOrBlocked()
        val hasSecondRunWhileHeld = hasSecondRun.get()
        releaseFirst.countDown()
        firstHolder.join(TIMEOUT_MILLIS)
        secondHolder.join(TIMEOUT_MILLIS)

        assertFalse(hasSecondRunWhileHeld)
        assertTrue(hasSecondRun.get())
    }

    private fun Thread.awaitFinishedOrBlocked() {
        var waitedMillis = 0L
        while (isAlive && state != Thread.State.BLOCKED && waitedMillis < TIMEOUT_MILLIS) {
            join(POLL_MILLIS)
            waitedMillis += POLL_MILLIS
        }
    }

    private companion object {
        private const val PREFERENCES_NAME = "bugle_participant_destinations"
        private const val READINGS_KEY = "readings"
        private const val LAST_ACTIVE_SUB_IDS_KEY = "last_active_sub_ids"
        private const val DATABASE_VERSION = 5
        private const val LAST_PARTICIPANT_ID = 42L
        private const val TIMEOUT_MILLIS = 10_000L
        private const val POLL_MILLIS = 10L

        private val SIM_HISTORY = SimHistory(
            lastActiveSubIds = setOf(2, 3),
            recordedReadings = mapOf(
                "211234" to RecordedReading(
                    subIds = setOf(3, 2),
                    canonicalDestination = "+298211234",
                ),
                "56789" to RecordedReading(subIds = setOf(4), canonicalDestination = null),
            ),
        )
    }
}
