package com.android.messaging.data.conversationstate.store

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.android.messaging.data.conversationstate.model.MirroredConversationState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface ConversationStatePreferencesStore {

    fun readMirroredState(): MirroredConversationState

    fun saveMirroredState(mirroredState: MirroredConversationState)

    fun isRestorePending(): Boolean

    fun markRestorePending()

    fun clearRestorePending()
}

internal class ConversationStatePreferencesStoreImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
) : ConversationStatePreferencesStore {

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override fun readMirroredState(): MirroredConversationState {
        return MirroredConversationState(
            blockedDestinations = preferences
                .getStringSet(BLOCKED_DESTINATIONS_KEY, null)
                .orEmpty(),
            archivedThreadIds = preferences
                .getStringSet(ARCHIVED_THREAD_IDS_KEY, null)
                .orEmpty(),
            mirroredAt = preferences.getLong(MIRRORED_AT_KEY, 0L),
            databaseVersion = preferences.getInt(DATABASE_VERSION_KEY, 0),
        )
    }

    override fun saveMirroredState(mirroredState: MirroredConversationState) {
        preferences.edit(commit = true) {
            putStringSet(BLOCKED_DESTINATIONS_KEY, mirroredState.blockedDestinations)
            putStringSet(ARCHIVED_THREAD_IDS_KEY, mirroredState.archivedThreadIds)
            putLong(MIRRORED_AT_KEY, mirroredState.mirroredAt)
            putInt(DATABASE_VERSION_KEY, mirroredState.databaseVersion)
        }
    }

    override fun isRestorePending(): Boolean {
        return preferences.getBoolean(RESTORE_PENDING_KEY, false)
    }

    override fun markRestorePending() {
        preferences.edit(commit = true) { putBoolean(RESTORE_PENDING_KEY, true) }
    }

    override fun clearRestorePending() {
        preferences.edit(commit = true) { remove(RESTORE_PENDING_KEY) }
    }

    private companion object {
        private const val PREFERENCES_NAME = "bugle_conversation_state"

        private const val BLOCKED_DESTINATIONS_KEY = "blocked_destinations"
        private const val ARCHIVED_THREAD_IDS_KEY = "archived_thread_ids"
        private const val MIRRORED_AT_KEY = "mirrored_at"
        private const val DATABASE_VERSION_KEY = "database_version"
        private const val RESTORE_PENDING_KEY = "restore_pending"
    }
}
