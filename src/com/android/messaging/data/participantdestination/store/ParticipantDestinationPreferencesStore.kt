package com.android.messaging.data.participantdestination.store

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.android.messaging.data.participantdestination.model.RecordedReading
import com.android.messaging.data.participantdestination.model.SimHistory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface ParticipantDestinationPreferencesStore {

    fun <T> withLock(block: () -> T): T

    fun readSimHistory(): SimHistory

    fun saveSimHistory(history: SimHistory)

    fun readNormalizedSubIds(): Set<Int>

    fun readNormalizedParticipantId(): Long

    fun readNormalizedDatabaseVersion(): Int

    fun saveNormalization(
        subIds: Set<Int>,
        databaseVersion: Int,
        lastParticipantId: Long,
        history: SimHistory,
    )

    fun clearNormalization()
}

internal class ParticipantDestinationPreferencesStoreImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
) : ParticipantDestinationPreferencesStore {

    private val lock = Any()

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override fun <T> withLock(block: () -> T): T {
        return synchronized(lock, block)
    }

    override fun readSimHistory(): SimHistory {
        return SimHistory(
            lastActiveSubIds = readSubIds(key = LAST_ACTIVE_SUB_IDS_KEY),
            recordedReadings = readRecordedReadings(),
        )
    }

    override fun saveSimHistory(history: SimHistory) {
        preferences.edit(commit = true) {
            putSimHistory(history = history)
        }
    }

    override fun readNormalizedSubIds(): Set<Int> {
        return readSubIds(key = NORMALIZED_SUB_IDS_KEY)
    }

    override fun readNormalizedParticipantId(): Long {
        return preferences.getLong(NORMALIZED_PARTICIPANT_ID_KEY, 0L)
    }

    override fun readNormalizedDatabaseVersion(): Int {
        return preferences.getInt(NORMALIZED_DATABASE_VERSION_KEY, 0)
    }

    override fun saveNormalization(
        subIds: Set<Int>,
        databaseVersion: Int,
        lastParticipantId: Long,
        history: SimHistory,
    ) {
        preferences.edit(commit = true) {
            putSubIds(key = NORMALIZED_SUB_IDS_KEY, subIds = subIds)
            putInt(NORMALIZED_DATABASE_VERSION_KEY, databaseVersion)
            putLong(NORMALIZED_PARTICIPANT_ID_KEY, lastParticipantId)
            putSimHistory(history = history)
        }
    }

    override fun clearNormalization() {
        preferences.edit(commit = true) {
            remove(NORMALIZED_SUB_IDS_KEY)
            remove(NORMALIZED_DATABASE_VERSION_KEY)
            remove(NORMALIZED_PARTICIPANT_ID_KEY)
        }
    }

    private fun SharedPreferences.Editor.putSimHistory(history: SimHistory) {
        putSubIds(key = LAST_ACTIVE_SUB_IDS_KEY, subIds = history.lastActiveSubIds)
        putStringSet(
            READINGS_KEY,
            history.recordedReadings.mapTo(mutableSetOf()) { (destination, reading) ->
                val subIds = reading.subIds.sorted().joinToString(separator = SUB_ID_SEPARATOR)
                val canonicalDestination = reading.canonicalDestination.orEmpty()
                "$subIds$READING_SEPARATOR$canonicalDestination$READING_SEPARATOR$destination"
            },
        )
    }

    private fun SharedPreferences.Editor.putSubIds(key: String, subIds: Set<Int>) {
        putStringSet(key, subIds.mapTo(mutableSetOf()) { subId -> subId.toString() })
    }

    private fun readSubIds(key: String): Set<Int> {
        return preferences.getStringSet(key, null).orEmpty().mapNotNullTo(mutableSetOf()) { subId ->
            subId.toIntOrNull()
        }
    }

    private fun readRecordedReadings(): Map<String, RecordedReading> {
        return preferences.getStringSet(READINGS_KEY, null).orEmpty()
            .map { reading -> reading.split(READING_SEPARATOR, limit = READING_PARTS) }
            .filter { parts -> parts.size == READING_PARTS }
            .associate { (subIds, canonicalDestination, destination) ->
                destination to RecordedReading(
                    subIds = subIds
                        .split(SUB_ID_SEPARATOR)
                        .mapNotNullTo(mutableSetOf()) { subId -> subId.toIntOrNull() },
                    canonicalDestination = canonicalDestination.ifEmpty { null },
                )
            }
    }

    private companion object {
        private const val PREFERENCES_NAME = "bugle_participant_destinations"

        private const val NORMALIZED_SUB_IDS_KEY = "normalized_sub_ids"
        private const val NORMALIZED_DATABASE_VERSION_KEY = "normalized_database_version"
        private const val NORMALIZED_PARTICIPANT_ID_KEY = "normalized_participant_id"
        private const val READINGS_KEY = "readings"
        private const val LAST_ACTIVE_SUB_IDS_KEY = "last_active_sub_ids"

        private const val READING_SEPARATOR = ":"
        private const val READING_PARTS = 3
        private const val SUB_ID_SEPARATOR = ","
    }
}
