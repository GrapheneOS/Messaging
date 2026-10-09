package com.android.messaging.data.conversationstate.store

import android.content.Context
import com.android.messaging.data.conversationstate.model.MirroredConversationState
import com.android.messaging.testutil.backupRulesExcluding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ConversationStatePreferencesStoreImplTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext

    @Test
    fun withNothingSaved_readsAnEmptyMirror() {
        val store = ConversationStatePreferencesStoreImpl(context = context)

        assertTrue(store.readMirroredState().isEmpty)
        assertEquals(0, store.readMirroredState().databaseVersion)
        assertFalse(store.isRestorePending())
    }

    @Test
    fun aSavedMirror_isReadBack() {
        ConversationStatePreferencesStoreImpl(context = context).saveMirroredState(
            mirroredState = MIRRORED_STATE,
        )

        assertEquals(
            MIRRORED_STATE,
            ConversationStatePreferencesStoreImpl(context = context).readMirroredState(),
        )
    }

    @Test
    fun aPendingRestore_staysPendingUntilCleared() {
        ConversationStatePreferencesStoreImpl(context = context).markRestorePending()

        val store = ConversationStatePreferencesStoreImpl(context = context)
        assertTrue(store.isRestorePending())

        store.clearRestorePending()
        assertFalse(store.isRestorePending())
    }

    @Test
    fun theMirror_isKeptInTheFileExcludedFromBackups() {
        ConversationStatePreferencesStoreImpl(context = context).apply {
            saveMirroredState(mirroredState = MIRRORED_STATE)
            markRestorePending()
        }

        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        assertEquals(
            setOf(
                "blocked_destinations",
                "archived_thread_ids",
                "mirrored_at",
                "database_version",
                "restore_pending",
            ),
            preferences.all.keys,
        )
        assertEquals(
            setOf("cloud-backup", "device-transfer"),
            backupRulesExcluding(context = context, sharedPreferencesName = PREFERENCES_NAME),
        )
    }

    private companion object {
        private const val PREFERENCES_NAME = "bugle_conversation_state"

        private val MIRRORED_STATE = MirroredConversationState(
            blockedDestinations = setOf("+15550100", "+15550101"),
            archivedThreadIds = setOf("12"),
            mirroredAt = 1_700_000_000_000L,
            databaseVersion = 5,
        )
    }
}
