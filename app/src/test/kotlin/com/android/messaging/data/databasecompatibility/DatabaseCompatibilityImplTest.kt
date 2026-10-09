package com.android.messaging.data.databasecompatibility

import com.android.messaging.data.databasecompatibility.model.RecordedDatabaseVersion
import com.android.messaging.data.databasecompatibility.store.DatabaseCompatibilityPreferencesStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseCompatibilityImplTest {

    private val preferencesStore = FakeDatabaseCompatibilityPreferencesStore()

    private val compatibility = DatabaseCompatibilityImpl(
        preferencesStore = preferencesStore,
        databaseVersion = DATABASE_VERSION,
    )

    @Test
    fun recordingTheVersion_marksItCompatibleOnlyWithItself() {
        compatibility.recordDatabaseVersion()

        assertEquals(
            RecordedDatabaseVersion(
                databaseVersion = DATABASE_VERSION,
                oldestCompatibleVersion = DATABASE_VERSION,
            ),
            preferencesStore.recordedVersion,
        )
    }

    @Test
    fun aNewerVersionCompatibleWithThisOne_keepsTheRows() {
        preferencesStore.recordedVersion = RecordedDatabaseVersion(
            databaseVersion = NEWER_VERSION,
            oldestCompatibleVersion = DATABASE_VERSION,
        )

        assertTrue(compatibility.canKeepRowsOnDowngradeFrom(newerVersion = NEWER_VERSION))
    }

    @Test
    fun aNewerVersionCompatibleOnlyWithNewerOnes_doesNotKeepTheRows() {
        preferencesStore.recordedVersion = RecordedDatabaseVersion(
            databaseVersion = NEWER_VERSION,
            oldestCompatibleVersion = NEWER_VERSION,
        )

        assertFalse(compatibility.canKeepRowsOnDowngradeFrom(newerVersion = NEWER_VERSION))
    }

    @Test
    fun withNothingRecorded_doesNotKeepTheRows() {
        assertFalse(compatibility.canKeepRowsOnDowngradeFrom(newerVersion = NEWER_VERSION))
    }

    @Test
    fun aRecordForAnotherVersion_doesNotKeepTheRows() {
        preferencesStore.recordedVersion = RecordedDatabaseVersion(
            databaseVersion = NEWER_VERSION,
            oldestCompatibleVersion = DATABASE_VERSION,
        )

        assertFalse(compatibility.canKeepRowsOnDowngradeFrom(newerVersion = NEWER_VERSION + 1))
    }

    private class FakeDatabaseCompatibilityPreferencesStore :
        DatabaseCompatibilityPreferencesStore {

        var recordedVersion: RecordedDatabaseVersion? = null

        override fun readRecordedVersion(): RecordedDatabaseVersion? {
            return recordedVersion
        }

        override fun saveRecordedVersion(recordedVersion: RecordedDatabaseVersion) {
            this.recordedVersion = recordedVersion
        }
    }

    private companion object {
        private const val DATABASE_VERSION = 5
        private const val NEWER_VERSION = 6
    }
}
