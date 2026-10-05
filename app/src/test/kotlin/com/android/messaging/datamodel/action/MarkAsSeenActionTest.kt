package com.android.messaging.datamodel.action

import android.content.Context
import android.database.Cursor
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.PendingIntentConstants
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
internal class MarkAsSeenActionTest {

    private lateinit var context: Context
    private lateinit var database: DatabaseWrapper

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext

        val dataModel = mockk<DataModel>(relaxed = true)
        installTestFactory(context = context, dataModel = dataModel)
        database = createInMemoryActionSyncTestDatabase(context)
        every { dataModel.database } returns database

        mockkStatic(BugleNotifications::class)
        every { BugleNotifications.cancel(any(), any()) } just Runs
        every { BugleNotifications.update(any<Int>()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun executeAction_withoutCancelling_marksAllSeenAndUpdatesNotifications() {
        insertUnseenMessage(
            conversationName = "First conversation",
            recipient = "+15551230000",
        )
        insertUnseenMessage(
            conversationName = "Second conversation",
            recipient = "+15551230001",
        )

        MarkAsSeenAction(null, false).executeAction()

        assertEquals(0, countUnseenMessages())
        verify(exactly = 1) {
            BugleNotifications.update(BugleNotifications.UPDATE_MESSAGES)
        }
        verify(exactly = 0) {
            BugleNotifications.cancel(any(), any())
        }
    }

    @Test
    fun executeAction_withoutChanges_doesNotUpdateOrCancelNotifications() {
        MarkAsSeenAction(null, false).executeAction()

        verify(exactly = 0) {
            BugleNotifications.update(any<Int>())
        }
        verify(exactly = 0) {
            BugleNotifications.cancel(any(), any())
        }
    }

    @Test
    fun publicConstructor_defaultsToCancellingNotifications() {
        MarkAsSeenAction(null as String?).executeAction()

        verify(exactly = 1) {
            BugleNotifications.cancel(PendingIntentConstants.SMS_NOTIFICATION_ID, null)
        }
        verify(exactly = 0) {
            BugleNotifications.update(any<Int>())
        }
    }

    private fun insertUnseenMessage(conversationName: String, recipient: String) {
        val participantId = database.insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                DatabaseHelper.ParticipantColumns.NORMALIZED_DESTINATION to recipient,
            ),
        )
        assertTrue("participant insert failed", participantId >= 0)

        val conversationId = database.insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(DatabaseHelper.ConversationColumns.NAME to conversationName),
        )
        assertTrue("conversation insert failed", conversationId >= 0)

        val messageId = database.insert(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                DatabaseHelper.MessageColumns.CONVERSATION_ID to conversationId,
                DatabaseHelper.MessageColumns.SENDER_PARTICIPANT_ID to participantId,
                DatabaseHelper.MessageColumns.SELF_PARTICIPANT_ID to participantId,
                DatabaseHelper.MessageColumns.STATUS to MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                DatabaseHelper.MessageColumns.SEEN to 0,
                DatabaseHelper.MessageColumns.READ to 0,
                DatabaseHelper.MessageColumns.RECEIVED_TIMESTAMP to 1L,
                DatabaseHelper.MessageColumns.SENT_TIMESTAMP to 1L,
            ),
        )
        assertTrue("message insert failed", messageId >= 0)
    }

    private fun countUnseenMessages(): Int {
        return database.query(
            DatabaseHelper.MESSAGES_TABLE,
            arrayOf("COUNT(*)"),
            "${DatabaseHelper.MessageColumns.SEEN}=0",
            null,
            null,
            null,
            null,
        ).use { cursor -> cursor.readCount() }
    }

    private fun Cursor.readCount(): Int {
        return when {
            moveToFirst() -> getInt(0)
            else -> 0
        }
    }
}
