package com.android.messaging.data.databasecompatibility.store

import android.content.Context
import com.android.messaging.data.databasecompatibility.model.RecordedDatabaseVersion
import com.android.messaging.testutil.backupRulesExcluding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DatabaseCompatibilityPreferencesStoreImplTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext

    @Test
    fun withNothingSaved_readsNoVersion() {
        val store = DatabaseCompatibilityPreferencesStoreImpl(context = context)

        assertNull(store.readRecordedVersion())
    }

    @Test
    fun aSavedVersion_isReadBack() {
        DatabaseCompatibilityPreferencesStoreImpl(context = context).saveRecordedVersion(
            recordedVersion = RECORDED_VERSION,
        )

        assertEquals(
            RECORDED_VERSION,
            DatabaseCompatibilityPreferencesStoreImpl(context = context).readRecordedVersion(),
        )
    }

    @Test
    fun theVersion_isKeptInTheFileExcludedFromBackups() {
        DatabaseCompatibilityPreferencesStoreImpl(context = context).saveRecordedVersion(
            recordedVersion = RECORDED_VERSION,
        )

        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        assertEquals(
            setOf("database_version", "oldest_compatible_version"),
            preferences.all.keys,
        )
        assertEquals(
            setOf("cloud-backup", "device-transfer"),
            backupRulesExcluding(context = context, sharedPreferencesName = PREFERENCES_NAME),
        )
    }

    private companion object {
        private const val PREFERENCES_NAME = "bugle_database_compatibility"

        private val RECORDED_VERSION = RecordedDatabaseVersion(
            databaseVersion = 6,
            oldestCompatibleVersion = 5,
        )
    }
}
