package com.android.messaging.datamodel.action

import android.database.Cursor
import android.database.MatrixCursor
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.ActionSyncTestDataModel
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.MessageNotificationState.FailedMessageQuery
import com.android.messaging.datamodel.SyncManager.ThreadInfoCache
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.mmslib.pdu.PduHeaders
import com.android.messaging.sms.DatabaseMessages
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = SyncMessagesActionMmsReplacementRaceTestApplication::class)
internal class SyncMessageBatchSeenTest {

    private lateinit var dataModel: ActionSyncTestDataModel
    private lateinit var database: DatabaseWrapper
    private lateinit var conversationId: String

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        dataModel = ActionSyncTestDataModel()
        installTestFactory(context = context, dataModel = dataModel)
        database = createInMemoryActionSyncTestDatabase(context)
        dataModel.database = database
        conversationId = database.insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(ConversationColumns.NAME to "Imported"),
        ).toString()
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun readSmsIsImportedAsSeen() {
        sync(smsToAdd = listOf(sms(isRead = true)))

        assertTrue(readImportedSeen())
    }

    @Test
    fun unreadSmsIsImportedAsUnseenSoItStillNotifies() {
        sync(smsToAdd = listOf(sms(isRead = false)))

        assertFalse(readImportedSeen())
    }

    @Test
    fun unreadSmsSyncedWhileInboxShowsNewestIsImportedAsSeen() {
        dataModel.isConversationListScrolledToNewestConversation = true

        sync(smsToAdd = listOf(sms(isRead = false)))

        assertTrue(readImportedSeen())
    }

    @Test
    fun readMmsIsImportedAsSeen() {
        sync(mmsToAdd = listOf(mms(isRead = true)))

        assertTrue(readImportedSeen())
    }

    // The provider marks every non-inbox row read, so read says nothing about a send failure.
    @Test
    fun failedOutgoingSmsStillNotifiesTheFailure() {
        dataModel.isConversationListScrolledToNewestConversation = true

        sync(smsToAdd = listOf(sms(isRead = true, type = Sms.MESSAGE_TYPE_FAILED)))

        assertEquals(1, countFailureNotificationCandidates())
    }

    @Test
    fun failedOutgoingMmsStillNotifiesTheFailure() {
        dataModel.isConversationListScrolledToNewestConversation = true

        sync(mmsToAdd = listOf(mms(isRead = true, messageBox = Mms.MESSAGE_BOX_OUTBOX)))

        assertEquals(1, countFailureNotificationCandidates())
    }

    private fun sync(
        smsToAdd: List<DatabaseMessages.SmsMessage> = emptyList(),
        mmsToAdd: List<DatabaseMessages.MmsMessage> = emptyList(),
    ) {
        val cache = mockk<ThreadInfoCache>(relaxed = true)
        every { cache.getOrCreateConversation(any(), any(), any(), any()) } returns conversationId
        val batch = SyncMessageBatch(ArrayList(smsToAdd), ArrayList(mmsToAdd), ArrayList(), cache)

        // The test DataModel hands out its database only off the main thread. The participant id
        // cache is static and would point at rows in an earlier test's database.
        var failure: Throwable? = null
        val worker = Thread {
            runCatching {
                BugleDatabaseOperations.clearParticipantIdCache()
                batch.updateLocalDatabase()
            }.onFailure { error -> failure = error }
        }
        worker.start()
        worker.join()
        failure?.let { error -> throw error }
    }

    private fun sms(
        isRead: Boolean,
        type: Int = Sms.MESSAGE_TYPE_INBOX,
    ): DatabaseMessages.SmsMessage {
        val cursor = telephonyCursor(
            projection = DatabaseMessages.SmsMessage.getProjection(),
            values = mapOf(
                Sms._ID to TELEPHONY_ROW_ID,
                Sms.TYPE to type,
                Sms.ADDRESS to SENDER,
                Sms.BODY to "Imported",
                Sms.DATE to TIMESTAMP_MILLIS,
                Sms.THREAD_ID to THREAD_ID,
                Sms.READ to isRead.toInt(),
                Sms.SEEN to 0,
                Sms.SUBSCRIPTION_ID to ParticipantData.DEFAULT_SELF_SUB_ID,
            ),
        )
        return cursor.use { DatabaseMessages.SmsMessage.get(it) }
    }

    private fun mms(
        isRead: Boolean,
        messageBox: Int = Mms.MESSAGE_BOX_INBOX,
    ): DatabaseMessages.MmsMessage {
        val messageType = when (messageBox) {
            Mms.MESSAGE_BOX_INBOX -> PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF
            else -> PduHeaders.MESSAGE_TYPE_SEND_REQ
        }
        val cursor = telephonyCursor(
            projection = DatabaseMessages.MmsMessage.getProjection(),
            values = mapOf(
                Mms._ID to TELEPHONY_ROW_ID,
                Mms.MESSAGE_BOX to messageBox,
                Mms.DATE to TIMESTAMP_MILLIS / 1_000L,
                Mms.THREAD_ID to THREAD_ID,
                Mms.READ to isRead.toInt(),
                Mms.SEEN to 0,
                Mms.MESSAGE_TYPE to messageType,
                Mms.SUBSCRIPTION_ID to ParticipantData.DEFAULT_SELF_SUB_ID,
            ),
        )
        return cursor.use { DatabaseMessages.MmsMessage.get(it) }.apply { setSender(SENDER) }
    }

    private fun telephonyCursor(projection: Array<String>, values: Map<String, Any?>): Cursor {
        return MatrixCursor(projection).apply {
            addRow(projection.map { column -> values[column] })
            moveToFirst()
        }
    }

    private fun readImportedSeen(): Boolean {
        return database.query(
            DatabaseHelper.MESSAGES_TABLE,
            arrayOf(MessageColumns.SEEN),
            null,
            null,
            null,
            null,
            null,
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            cursor.getInt(0) == 1
        }
    }

    private fun countFailureNotificationCandidates(): Int {
        return database.query(
            DatabaseHelper.MESSAGES_TABLE,
            arrayOf(MessageColumns.SEEN),
            FailedMessageQuery.FAILED_MESSAGES_WHERE_CLAUSE,
            null,
            null,
            null,
            null,
        ).use { cursor -> cursor.count }
    }

    private fun Boolean.toInt(): Int {
        return when {
            this -> 1
            else -> 0
        }
    }

    private companion object {
        private const val TELEPHONY_ROW_ID = 324L
        private const val THREAD_ID = 3_240L
        private const val SENDER = "+15553240000"
        private const val TIMESTAMP_MILLIS = 1_780_920_000_000L
    }
}
