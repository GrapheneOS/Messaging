package com.android.messaging.data.databasecompatibility.store

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.android.messaging.data.databasecompatibility.model.RecordedDatabaseVersion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface DatabaseCompatibilityPreferencesStore {

    fun readRecordedVersion(): RecordedDatabaseVersion?

    fun saveRecordedVersion(recordedVersion: RecordedDatabaseVersion)
}

internal class DatabaseCompatibilityPreferencesStoreImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
) : DatabaseCompatibilityPreferencesStore {

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override fun readRecordedVersion(): RecordedDatabaseVersion? {
        val isRecorded = preferences.contains(DATABASE_VERSION_KEY) &&
            preferences.contains(OLDEST_COMPATIBLE_VERSION_KEY)
        return when {
            isRecorded -> RecordedDatabaseVersion(
                databaseVersion = preferences.getInt(DATABASE_VERSION_KEY, 0),
                oldestCompatibleVersion = preferences.getInt(OLDEST_COMPATIBLE_VERSION_KEY, 0),
            )
            else -> null
        }
    }

    override fun saveRecordedVersion(recordedVersion: RecordedDatabaseVersion) {
        preferences.edit(commit = true) {
            putInt(DATABASE_VERSION_KEY, recordedVersion.databaseVersion)
            putInt(OLDEST_COMPATIBLE_VERSION_KEY, recordedVersion.oldestCompatibleVersion)
        }
    }

    private companion object {
        private const val PREFERENCES_NAME = "bugle_database_compatibility"

        private const val DATABASE_VERSION_KEY = "database_version"
        private const val OLDEST_COMPATIBLE_VERSION_KEY = "oldest_compatible_version"
    }
}
