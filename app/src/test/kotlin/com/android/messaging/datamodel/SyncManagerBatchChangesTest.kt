package com.android.messaging.datamodel

import com.android.messaging.datamodel.action.SyncBatchChanges
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SyncManagerBatchChangesTest {

    private val syncManager = SyncManager()

    @Test
    fun changesAreTakenOnceByTheActionThatFoundThem() {
        syncManager.startSyncBatch(UPPER_BOUND)
        val changes = createChanges(actionKey = ACTION_KEY)
        syncManager.setBatchChanges(UPPER_BOUND, changes)

        assertNull(syncManager.takeBatchChanges("other-action"))
        assertSame(changes, syncManager.takeBatchChanges(ACTION_KEY))
        assertNull(syncManager.takeBatchChanges(ACTION_KEY))
    }

    @Test
    fun changesOfAnOrphanBatchAreDropped() {
        syncManager.startSyncBatch(UPPER_BOUND)

        syncManager.setBatchChanges(UPPER_BOUND - 1, createChanges(actionKey = ACTION_KEY))

        assertNull(syncManager.takeBatchChanges(ACTION_KEY))
    }

    @Test
    fun endingTheBatchDropsUnclaimedChanges() {
        syncManager.startSyncBatch(UPPER_BOUND)
        syncManager.setBatchChanges(UPPER_BOUND, createChanges(actionKey = ACTION_KEY))

        syncManager.isBatchDirty(LOWER_BOUND)

        assertNull(syncManager.takeBatchChanges(ACTION_KEY))
    }

    @Test
    fun startingABatchDropsChangesOfThePreviousOne() {
        syncManager.startSyncBatch(UPPER_BOUND)
        syncManager.setBatchChanges(UPPER_BOUND, createChanges(actionKey = ACTION_KEY))
        syncManager.complete()

        syncManager.startSyncBatch(UPPER_BOUND)

        assertNull(syncManager.takeBatchChanges(ACTION_KEY))
    }

    @Test
    fun completingTheSyncEndsTheBatchInProgress() {
        assertTrue(syncManager.shouldSync(false, START_TIMESTAMP))
        syncManager.startSyncBatch(UPPER_BOUND)
        syncManager.setBatchChanges(UPPER_BOUND, createChanges(actionKey = ACTION_KEY))

        syncManager.complete()

        assertFalse(syncManager.isSyncing())
        assertFalse(syncManager.isSyncing(UPPER_BOUND))
        assertNull(syncManager.takeBatchChanges(ACTION_KEY))
        assertTrue(syncManager.shouldSync(false, START_TIMESTAMP + 1))
        syncManager.startSyncBatch(UPPER_BOUND + 1)
        assertTrue(syncManager.isSyncing(UPPER_BOUND + 1))
    }

    private fun createChanges(actionKey: String): SyncBatchChanges {
        return SyncBatchChanges(
            actionKey = actionKey,
            smsToAdd = emptyList(),
            mmsToAdd = emptyList(),
            messagesToDelete = mutableListOf(),
        )
    }

    private companion object {
        private const val ACTION_KEY = "sync-action"
        private const val LOWER_BOUND = 1_000L
        private const val UPPER_BOUND = 2_000L
        private const val START_TIMESTAMP = 3_000L
    }
}
