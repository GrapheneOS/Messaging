package com.android.messaging.data.conversationstate

import com.android.messaging.data.conversationstate.model.MirroredConversationState
import com.android.messaging.data.conversationstate.store.ConversationStateDatabaseStore
import com.android.messaging.data.conversationstate.store.ConversationStatePreferencesStore
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.BuglePrefsKeys
import com.android.messaging.util.LogUtil
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConversationStateMirrorImplTest {

    private val preferencesStore = FakeConversationStatePreferencesStore()
    private val databaseStore = FakeConversationStateDatabaseStore()
    private val recordedBlocks = mutableListOf<Set<String>>()
    private val applicationPrefs = FakeBuglePrefs()
    private var blockedNumberRecorder = BlockedNumberRecorder { destinations ->
        recordedBlocks += destinations
    }

    private val mirror: ConversationStateMirror by lazy {
        ConversationStateMirrorImpl(
            preferencesStore = preferencesStore,
            databaseStore = databaseStore,
            blockedNumberRecorder = blockedNumberRecorder,
            databaseVersion = DATABASE_VERSION,
        )
    }

    @Before
    fun setUp() {
        mockkStatic(LogUtil::class)
        every { LogUtil.w(any(), any(), any()) } just runs
        mockkStatic(BuglePrefs::class)
        every { BuglePrefs.getApplicationPrefs() } returns applicationPrefs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun theTablesCreatedWithAMirror_markARestorePending() {
        preferencesStore.mirroredState = MIRRORED_STATE

        mirror.onDatabaseCreated()

        assertTrue(mirror.isRestorePending())
    }

    @Test
    fun theTablesCreatedWithAnEmptyMirror_markNoRestore() {
        preferencesStore.mirroredState = MIRRORED_STATE.copy(
            blockedDestinations = emptySet(),
            archivedThreadIds = emptySet(),
        )

        mirror.onDatabaseCreated()

        assertFalse(mirror.isRestorePending())
    }

    @Test
    fun anUpgradeFromBelowTheMirroredVersion_marksARestorePending() {
        preferencesStore.mirroredState = MIRRORED_STATE

        mirror.onDatabaseUpgraded(oldVersion = DATABASE_VERSION - 1)

        assertTrue(mirror.isRestorePending())
    }

    @Test
    fun anUpgradeFromTheMirroredVersion_marksNoRestore() {
        preferencesStore.mirroredState = MIRRORED_STATE

        mirror.onDatabaseUpgraded(oldVersion = DATABASE_VERSION)

        assertFalse(mirror.isRestorePending())
    }

    @Test
    fun anUpdate_savesTheDatabaseStateWithTheTimeAndVersion() {
        databaseStore.blockedDestinations = setOf(BLOCKED_DESTINATION)
        databaseStore.archivedThreadIds = setOf(ARCHIVED_THREAD_ID)

        val updateStartedAt = System.currentTimeMillis()
        mirror.update()
        val updateEndedAt = System.currentTimeMillis()

        val mirroredState = preferencesStore.mirroredState
        assertEquals(setOf(BLOCKED_DESTINATION), mirroredState.blockedDestinations)
        assertEquals(setOf(ARCHIVED_THREAD_ID), mirroredState.archivedThreadIds)
        assertEquals(DATABASE_VERSION, mirroredState.databaseVersion)
        assertTrue(mirroredState.mirroredAt in updateStartedAt..updateEndedAt)
    }

    @Test
    fun anUpdateWhileARestoreIsPending_keepsTheMirrorToRestore() {
        preferencesStore.mirroredState = MIRRORED_STATE
        preferencesStore.hasPendingRestore = true

        mirror.update()

        assertEquals(MIRRORED_STATE, preferencesStore.mirroredState)
    }

    @Test
    fun anUpdate_recordsTheNumbersBlockedSinceTheLastMirror() {
        preferencesStore.mirroredState = MIRRORED_STATE
        databaseStore.blockedDestinations = setOf(BLOCKED_DESTINATION, NEWLY_BLOCKED_DESTINATION)

        mirror.update()

        assertEquals(listOf(setOf(NEWLY_BLOCKED_DESTINATION)), recordedBlocks)
    }

    @Test
    fun anUpdateWhileARestoreIsPending_stillRecordsTheNumbersBlockedSince() {
        preferencesStore.mirroredState = MIRRORED_STATE
        preferencesStore.hasPendingRestore = true
        databaseStore.blockedDestinations = setOf(NEWLY_BLOCKED_DESTINATION)

        mirror.update()

        assertEquals(listOf(setOf(NEWLY_BLOCKED_DESTINATION)), recordedBlocks)
    }

    @Test
    fun anUpdateWithNoNewBlock_recordsNothing() {
        preferencesStore.mirroredState = MIRRORED_STATE
        databaseStore.blockedDestinations = setOf(BLOCKED_DESTINATION)

        mirror.update()

        assertEquals(emptyList<Set<String>>(), recordedBlocks)
    }

    @Test
    fun aFailedRecording_leavesTheMirrorUnsaved() {
        blockedNumberRecorder = BlockedNumberRecorder {
            throw IllegalStateException(DATABASE_FULL)
        }
        preferencesStore.mirroredState = MIRRORED_STATE
        databaseStore.blockedDestinations = setOf(BLOCKED_DESTINATION, NEWLY_BLOCKED_DESTINATION)

        mirror.update()

        assertEquals(MIRRORED_STATE, preferencesStore.mirroredState)
    }

    @Test
    fun aFailedUpdate_isOnlyLogged() {
        databaseStore.onReadArchivedThreadIds = {
            throw IllegalStateException(DATABASE_FULL)
        }

        mirror.update()

        verify(exactly = 1) { LogUtil.w(any(), any(), any<IllegalStateException>()) }
    }

    @Test
    fun withNoRestorePending_restoresNothing() {
        preferencesStore.mirroredState = MIRRORED_STATE

        assertFalse(mirror.restoreIfDue())
        assertEquals(emptyList<MirroredConversationState>(), databaseStore.restoredStates)
    }

    @Test
    fun beforeTheFirstFullSyncCompletes_restoresNothing() {
        preferencesStore.mirroredState = MIRRORED_STATE
        preferencesStore.hasPendingRestore = true

        assertFalse(mirror.restoreIfDue())
        assertEquals(emptyList<MirroredConversationState>(), databaseStore.restoredStates)
        assertTrue(mirror.isRestorePending())
    }

    @Test
    fun onceTheFirstFullSyncCompletes_restoresTheMirrorOnce() {
        preferencesStore.mirroredState = MIRRORED_STATE
        preferencesStore.hasPendingRestore = true
        applicationPrefs.putLong(BuglePrefsKeys.LAST_FULL_SYNC_TIME, FULL_SYNC_TIME)

        assertTrue(mirror.restoreIfDue())
        assertFalse(mirror.restoreIfDue())

        assertEquals(listOf(MIRRORED_STATE), databaseStore.restoredStates)
        assertFalse(mirror.isRestorePending())
    }

    @Test
    fun aFailedRestore_staysPending() {
        preferencesStore.mirroredState = MIRRORED_STATE
        preferencesStore.hasPendingRestore = true
        applicationPrefs.putLong(BuglePrefsKeys.LAST_FULL_SYNC_TIME, FULL_SYNC_TIME)
        databaseStore.restoreFailure = IllegalStateException(DATABASE_FULL)

        assertThrows(IllegalStateException::class.java) { mirror.restoreIfDue() }

        assertTrue(mirror.isRestorePending())
    }

    @Test
    fun aMirrorThatReadBeforeABlock_doesNotSaveOverTheBlock() {
        val isOlderMirrorReading = CountDownLatch(1)
        val resumeOlderMirror = CountDownLatch(1)
        databaseStore.onReadArchivedThreadIds = {
            if (isOlderMirrorReading.count > 0) {
                isOlderMirrorReading.countDown()
                resumeOlderMirror.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            }
        }
        val olderMirror = thread { mirror.update() }
        assertTrue(isOlderMirrorReading.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))

        databaseStore.blockedDestinations = setOf(BLOCKED_DESTINATION)
        val blockMirror = thread { mirror.update() }
        blockMirror.awaitFinishedOrBlocked()
        resumeOlderMirror.countDown()
        olderMirror.join(TIMEOUT_MILLIS)
        blockMirror.join(TIMEOUT_MILLIS)

        assertEquals(
            setOf(BLOCKED_DESTINATION),
            preferencesStore.mirroredState.blockedDestinations,
        )
    }

    private fun Thread.awaitFinishedOrBlocked() {
        var waitedMillis = 0L
        while (isAlive && state != Thread.State.BLOCKED && waitedMillis < TIMEOUT_MILLIS) {
            join(POLL_MILLIS)
            waitedMillis += POLL_MILLIS
        }
    }

    private class FakeConversationStatePreferencesStore : ConversationStatePreferencesStore {

        @Volatile
        var mirroredState = MirroredConversationState(
            blockedDestinations = emptySet(),
            archivedThreadIds = emptySet(),
            mirroredAt = 0L,
            databaseVersion = 0,
        )

        @Volatile
        var hasPendingRestore = false

        override fun readMirroredState(): MirroredConversationState {
            return mirroredState
        }

        override fun saveMirroredState(mirroredState: MirroredConversationState) {
            this.mirroredState = mirroredState
        }

        override fun isRestorePending(): Boolean {
            return hasPendingRestore
        }

        override fun markRestorePending() {
            hasPendingRestore = true
        }

        override fun clearRestorePending() {
            hasPendingRestore = false
        }
    }

    private class FakeConversationStateDatabaseStore : ConversationStateDatabaseStore {

        @Volatile
        var blockedDestinations = emptySet<String>()

        @Volatile
        var archivedThreadIds = emptySet<String>()

        @Volatile
        var onReadArchivedThreadIds: () -> Unit = {}

        var restoreFailure: RuntimeException? = null

        val restoredStates = mutableListOf<MirroredConversationState>()

        override fun readBlockedDestinations(): Set<String> {
            return blockedDestinations
        }

        override fun readArchivedThreadIds(): Set<String> {
            onReadArchivedThreadIds()
            return archivedThreadIds
        }

        override fun restore(mirroredState: MirroredConversationState) {
            restoreFailure?.let { failure -> throw failure }
            restoredStates += mirroredState
        }
    }

    private companion object {
        private const val DATABASE_FULL = "database or disk is full"
        private const val BLOCKED_DESTINATION = "+15550100"
        private const val NEWLY_BLOCKED_DESTINATION = "+15550101"
        private const val ARCHIVED_THREAD_ID = "12"
        private const val DATABASE_VERSION = 5
        private const val FULL_SYNC_TIME = 2_000L
        private const val TIMEOUT_MILLIS = 10_000L
        private const val POLL_MILLIS = 10L

        private val MIRRORED_STATE = MirroredConversationState(
            blockedDestinations = setOf(BLOCKED_DESTINATION),
            archivedThreadIds = setOf(ARCHIVED_THREAD_ID),
            mirroredAt = 1_000L,
            databaseVersion = DATABASE_VERSION,
        )
    }
}
