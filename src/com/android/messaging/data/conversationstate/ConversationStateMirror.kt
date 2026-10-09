package com.android.messaging.data.conversationstate

import android.content.Context
import com.android.messaging.data.conversationstate.model.MirroredConversationState
import com.android.messaging.data.conversationstate.store.ConversationStateDatabaseStore
import com.android.messaging.data.conversationstate.store.ConversationStatePreferencesStore
import com.android.messaging.di.core.DatabaseVersion
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.BuglePrefsKeys
import com.android.messaging.util.LogUtil
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

internal interface ConversationStateMirror {

    fun isRestorePending(): Boolean

    fun onDatabaseCreated()

    fun onDatabaseUpgraded(oldVersion: Int)

    fun update()

    fun restoreIfDue(): Boolean

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Provider {
        fun conversationStateMirror(): ConversationStateMirror
    }

    companion object {

        @JvmStatic
        fun get(context: Context): ConversationStateMirror {
            return EntryPointAccessors
                .fromApplication(context, Provider::class.java)
                .conversationStateMirror()
        }
    }
}

@Singleton
internal class ConversationStateMirrorImpl @Inject constructor(
    private val preferencesStore: ConversationStatePreferencesStore,
    private val databaseStore: ConversationStateDatabaseStore,
    private val blockedNumberRecorder: BlockedNumberRecorder,
    @param:DatabaseVersion
    private val databaseVersion: Int,
) : ConversationStateMirror {

    private val lock = Any()

    override fun isRestorePending(): Boolean {
        return preferencesStore.isRestorePending()
    }

    override fun onDatabaseCreated() {
        if (!preferencesStore.readMirroredState().isEmpty) {
            preferencesStore.markRestorePending()
        }
    }

    override fun onDatabaseUpgraded(oldVersion: Int) {
        if (preferencesStore.readMirroredState().databaseVersion > oldVersion) {
            preferencesStore.markRestorePending()
        }
    }

    override fun update() {
        synchronized(lock) {
            runCatching { mirror() }.onFailure { exception ->
                LogUtil.w(TAG, "Couldn't mirror the conversation state", exception)
            }
        }
    }

    override fun restoreIfDue(): Boolean {
        return synchronized(lock) {
            val isRestoreDue = preferencesStore.isRestorePending() && isFullSyncDone()
            if (isRestoreDue) {
                databaseStore.restore(mirroredState = preferencesStore.readMirroredState())
                preferencesStore.clearRestorePending()
            }
            isRestoreDue
        }
    }

    private fun isFullSyncDone(): Boolean {
        val lastFullSyncTime = BuglePrefs.getApplicationPrefs().getLong(
            BuglePrefsKeys.LAST_FULL_SYNC_TIME,
            BuglePrefsKeys.LAST_FULL_SYNC_TIME_DEFAULT,
        )
        return lastFullSyncTime != BuglePrefsKeys.LAST_FULL_SYNC_TIME_DEFAULT
    }

    private fun mirror() {
        val blockedDestinations = databaseStore.readBlockedDestinations()
        val newlyBlockedDestinations =
            blockedDestinations - preferencesStore.readMirroredState().blockedDestinations
        if (newlyBlockedDestinations.isNotEmpty()) {
            blockedNumberRecorder.record(destinations = newlyBlockedDestinations)
        }
        if (!preferencesStore.isRestorePending()) {
            preferencesStore.saveMirroredState(
                mirroredState = MirroredConversationState(
                    blockedDestinations = blockedDestinations,
                    archivedThreadIds = databaseStore.readArchivedThreadIds(),
                    mirroredAt = System.currentTimeMillis(),
                    databaseVersion = databaseVersion,
                ),
            )
        }
    }

    private companion object {
        private const val TAG = "ConversationStateMirror"
    }
}
