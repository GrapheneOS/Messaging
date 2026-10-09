package com.android.messaging.data.conversationstate.store

import android.content.Context
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversationstate.model.MirroredConversationState
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ConversationStateDatabaseStoreImplTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext
    private lateinit var db: DatabaseWrapper
    private val store = ConversationStateDatabaseStoreImpl()

    @Before
    fun setUp() {
        installTestFactory(
            context = context,
            dataModel = mockk<DataModel>(relaxed = true) {
                every { database } answers { db }
            },
        )
        db = createInMemoryActionSyncTestDatabase(context = context)
    }

    @After
    fun tearDown() {
        db.database.close()
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun readBlockedDestinations_readsTheBlockedSenders() {
        db.insertSyncedConversation(
            destination = BLOCKED_DESTINATION,
            threadId = BLOCKED_THREAD_ID,
            isBlocked = true,
        )
        db.insertSyncedConversation(destination = OTHER_DESTINATION, threadId = OTHER_THREAD_ID)

        assertEquals(setOf(BLOCKED_DESTINATION), store.readBlockedDestinations())
    }

    @Test
    fun readBlockedDestinations_leavesOutTheSelfParticipants() {
        db.insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.SUB_ID to SELF_SUB_ID,
                ParticipantColumns.NORMALIZED_DESTINATION to SELF_DESTINATION,
                ParticipantColumns.BLOCKED to 1,
            ),
        )

        assertEquals(emptySet<String>(), store.readBlockedDestinations())
    }

    @Test
    fun readArchivedThreadIds_readsTheArchivedSyncedThreads() {
        db.insertSyncedConversation(
            destination = ARCHIVED_DESTINATION,
            threadId = ARCHIVED_THREAD_ID,
            isArchived = true,
        )
        db.insertSyncedConversation(destination = OTHER_DESTINATION, threadId = OTHER_THREAD_ID)
        db.insertSyncedConversation(
            destination = BLOCKED_DESTINATION,
            threadId = NO_THREAD_ID,
            isArchived = true,
        )

        assertEquals(setOf(ARCHIVED_THREAD_ID.toString()), store.readArchivedThreadIds())
    }

    @Test
    fun restore_blocksAndArchivesTheResyncedConversations() {
        insertResyncedConversations(sortTimestamp = SYNCED_SORT_TIMESTAMP)

        store.restore(mirroredState = MIRRORED_STATE)

        assertTrue(db.isBlocked(destination = BLOCKED_DESTINATION))
        assertTrue(db.isArchived(threadId = BLOCKED_THREAD_ID))
        assertTrue(db.isArchived(threadId = ARCHIVED_THREAD_ID))
        assertFalse(db.isBlocked(destination = OTHER_DESTINATION))
        assertFalse(db.isArchived(threadId = OTHER_THREAD_ID))
    }

    @Test
    fun restore_createsTheParticipantOfABlockedSenderWithNoConversation() {
        store.restore(mirroredState = MIRRORED_STATE)

        assertTrue(db.isBlocked(destination = BLOCKED_DESTINATION))
    }

    @Test
    fun restore_leavesConversationsActiveSinceUnarchivedUnlessTheSenderIsBlocked() {
        insertResyncedConversations(sortTimestamp = MIRRORED_AT_MILLIS + 1)

        store.restore(mirroredState = MIRRORED_STATE)

        assertTrue(db.isArchived(threadId = BLOCKED_THREAD_ID))
        assertFalse(db.isArchived(threadId = ARCHIVED_THREAD_ID))
    }

    private fun insertResyncedConversations(sortTimestamp: Long) {
        db.insertSyncedConversation(
            destination = BLOCKED_DESTINATION,
            threadId = BLOCKED_THREAD_ID,
            sortTimestamp = sortTimestamp,
        )
        db.insertSyncedConversation(
            destination = ARCHIVED_DESTINATION,
            threadId = ARCHIVED_THREAD_ID,
            sortTimestamp = sortTimestamp,
        )
        db.insertSyncedConversation(
            destination = OTHER_DESTINATION,
            threadId = OTHER_THREAD_ID,
            sortTimestamp = sortTimestamp,
        )
    }

    private fun DatabaseWrapper.insertSyncedConversation(
        destination: String,
        threadId: Long,
        isBlocked: Boolean = false,
        isArchived: Boolean = false,
        sortTimestamp: Long = SYNCED_SORT_TIMESTAMP,
    ) {
        val participantId = insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.SUB_ID to ParticipantData.OTHER_THAN_SELF_SUB_ID,
                ParticipantColumns.NORMALIZED_DESTINATION to destination,
                ParticipantColumns.SEND_DESTINATION to destination,
                ParticipantColumns.DISPLAY_DESTINATION to destination,
                ParticipantColumns.BLOCKED to if (isBlocked) 1 else 0,
            ),
        )
        val conversationId = insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.SMS_THREAD_ID to threadId,
                ConversationColumns.ARCHIVE_STATUS to if (isArchived) 1 else 0,
                ConversationColumns.SORT_TIMESTAMP to sortTimestamp,
                ConversationColumns.PARTICIPANT_COUNT to 1,
                ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION to destination,
            ),
        )
        insert(
            DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ConversationParticipantsColumns.CONVERSATION_ID to conversationId,
                ConversationParticipantsColumns.PARTICIPANT_ID to participantId,
            ),
        )
    }

    private fun DatabaseWrapper.isBlocked(destination: String): Boolean {
        return rawQuery(
            "SELECT COUNT(*) FROM ${DatabaseHelper.PARTICIPANTS_TABLE} WHERE " +
                "${ParticipantColumns.NORMALIZED_DESTINATION}=? AND " +
                "${ParticipantColumns.SUB_ID}=${ParticipantData.OTHER_THAN_SELF_SUB_ID} AND " +
                "${ParticipantColumns.BLOCKED}=1",
            arrayOf(destination),
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0) == 1
        }
    }

    private fun DatabaseWrapper.isArchived(threadId: Long): Boolean {
        return rawQuery(
            "SELECT ${ConversationColumns.ARCHIVE_STATUS} FROM " +
                "${DatabaseHelper.CONVERSATIONS_TABLE} WHERE ${ConversationColumns.SMS_THREAD_ID}=?",
            arrayOf(threadId.toString()),
        ).use { cursor ->
            assertEquals("no conversation for thread $threadId", 1, cursor.count)
            cursor.moveToFirst()
            cursor.getInt(0) == 1
        }
    }

    private companion object {
        private const val BLOCKED_DESTINATION = "+15550100"
        private const val ARCHIVED_DESTINATION = "+15550101"
        private const val OTHER_DESTINATION = "+15550102"
        private const val SELF_DESTINATION = "+15550199"
        private const val SELF_SUB_ID = 1
        private const val BLOCKED_THREAD_ID = 11L
        private const val ARCHIVED_THREAD_ID = 12L
        private const val OTHER_THREAD_ID = 13L
        private const val NO_THREAD_ID = 0L
        private const val MIRRORED_AT_MILLIS = 2_000L

        private const val SYNCED_SORT_TIMESTAMP = 1_000L

        private val MIRRORED_STATE = MirroredConversationState(
            blockedDestinations = setOf(BLOCKED_DESTINATION),
            archivedThreadIds = setOf(BLOCKED_THREAD_ID.toString(), ARCHIVED_THREAD_ID.toString()),
            mirroredAt = MIRRORED_AT_MILLIS,
            databaseVersion = 1,
        )
    }
}
