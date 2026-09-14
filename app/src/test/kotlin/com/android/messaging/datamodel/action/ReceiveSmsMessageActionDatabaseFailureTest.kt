package com.android.messaging.datamodel.action

import android.content.ContentValues
import android.database.sqlite.SQLiteException
import android.provider.Telephony.Sms
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.sms.MmsSmsUtils
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReceiveSmsMessageActionDatabaseFailureTest {

    private val dataModel = mockk<DataModel>(relaxed = true)

    @Before
    fun setUp() {
        installTestFactory(
            context = RuntimeEnvironment.getApplication().applicationContext,
            dataModel = dataModel,
        )
        mockkStatic(MmsSmsUtils.Threads::class)
        every { MmsSmsUtils.Threads.getOrCreateThreadId(any(), any<String>()) } returns THREAD_ID
        mockkStatic(BugleDatabaseOperations::class)
        every { BugleDatabaseOperations.isBlockedDestination(any(), any()) } returns false
        every {
            BugleDatabaseOperations.getOrCreateConversationFromRecipient(any(), any(), any(), any())
        } returns CONVERSATION_ID
        every {
            BugleDatabaseOperations.getOrCreateParticipantInTransaction(any(), any())
        } throws SQLiteException("Transient participant lookup failure")
        mockkStatic(BugleNotifications::class)
        every { BugleNotifications.update(any(), any()) } just runs
        mockkStatic(ProcessPendingMessagesAction::class)
        every {
            ProcessPendingMessagesAction.scheduleProcessPendingMessagesAction(any(), any())
        } just runs
        mockkStatic(MessagingContentProvider::class)
        every { MessagingContentProvider.notifyMessagesChanged(any()) } just runs
        every { MessagingContentProvider.notifyPartsChanged() } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun receiveSmsStillNotifiesAndSchedulesProcessingWhenDatabaseInsertFails() {
        val action = TestReceiveSmsMessageAction(receivedSmsValues())

        val result = action.execute()

        assertNull(result)
        verify(exactly = 1) {
            BugleNotifications.update(CONVERSATION_ID, BugleNotifications.UPDATE_ALL)
        }
        verify(exactly = 1) {
            ProcessPendingMessagesAction.scheduleProcessPendingMessagesAction(false, action)
        }
        verify(exactly = 1) { MessagingContentProvider.notifyMessagesChanged(CONVERSATION_ID) }
    }

    private fun receivedSmsValues(): ContentValues {
        return contentValuesOf(
            Sms.ADDRESS to SENDER,
            Sms.BODY to MESSAGE_TEXT,
            Sms.DATE to RECEIVED_TIMESTAMP_MILLIS,
            Sms.DATE_SENT to SENT_TIMESTAMP_MILLIS,
            Sms.REPLY_PATH_PRESENT to 0,
            Sms.SUBSCRIPTION_ID to ParticipantData.DEFAULT_SELF_SUB_ID,
            Sms.Inbox.READ to 0,
            Sms.Inbox.SEEN to 0,
        )
    }

    private class TestReceiveSmsMessageAction(
        messageValues: ContentValues,
    ) : ReceiveSmsMessageAction(messageValues) {
        fun execute(): Any? {
            return executeAction()
        }
    }

    private companion object {
        private const val SENDER = "+15551234567"
        private const val MESSAGE_TEXT = "Incoming message"
        private const val CONVERSATION_ID = "193"
        private const val THREAD_ID = 193L
        private const val RECEIVED_TIMESTAMP_MILLIS = 1_780_920_000_000L
        private const val SENT_TIMESTAMP_MILLIS = 1_780_919_999_000L
    }
}
