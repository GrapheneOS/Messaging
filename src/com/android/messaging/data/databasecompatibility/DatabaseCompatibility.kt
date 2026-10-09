package com.android.messaging.data.databasecompatibility

import androidx.annotation.VisibleForTesting
import com.android.messaging.data.databasecompatibility.model.RecordedDatabaseVersion
import com.android.messaging.data.databasecompatibility.store.DatabaseCompatibilityPreferencesStore
import com.android.messaging.di.core.DatabaseVersion
import javax.inject.Inject

internal interface DatabaseCompatibility {

    fun recordDatabaseVersion()

    fun canKeepRowsOnDowngradeFrom(newerVersion: Int): Boolean
}

internal class DatabaseCompatibilityImpl @Inject constructor(
    private val preferencesStore: DatabaseCompatibilityPreferencesStore,
    @param:DatabaseVersion
    private val databaseVersion: Int,
) : DatabaseCompatibility {

    override fun recordDatabaseVersion() {
        preferencesStore.saveRecordedVersion(
            recordedVersion = RecordedDatabaseVersion(
                databaseVersion = databaseVersion,
                oldestCompatibleVersion = OLDEST_COMPATIBLE_VERSIONS[databaseVersion]
                    ?: databaseVersion,
            ),
        )
    }

    override fun canKeepRowsOnDowngradeFrom(newerVersion: Int): Boolean {
        return when (val recordedVersion = preferencesStore.readRecordedVersion()) {
            null -> false
            else ->
                recordedVersion.databaseVersion == newerVersion &&
                    recordedVersion.oldestCompatibleVersion <= databaseVersion
        }
    }

    internal companion object {
        // Database versions that older versions still read and write correctly, mapped to the
        // oldest such version. A downgrade keeps their tables and columns, so the upgrade to
        // them has to run again cleanly over its own result
        @VisibleForTesting
        internal val OLDEST_COMPATIBLE_VERSIONS = emptyMap<Int, Int>()
    }
}
