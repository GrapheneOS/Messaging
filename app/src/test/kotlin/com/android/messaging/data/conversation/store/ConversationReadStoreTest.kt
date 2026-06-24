package com.android.messaging.data.conversation.store

import android.content.ContentValues
import android.database.sqlite.SQLiteStatement
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.sms.MmsUtils
import com.android.messaging.util.PendingIntentConstants
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationReadStoreTest {

    private val database = mockk<DatabaseWrapper>(relaxed = true)
    private val dataModel = mockk<DataModel>()
    private val latestMessageStatement = mockk<SQLiteStatement>()

    private val store = ConversationReadStoreImpl()

    @Before
    fun setUp() {
        mockkStatic(DataModel::class)
        mockkStatic(BugleDatabaseOperations::class)
        mockkStatic(BugleNotifications::class)
        mockkStatic(MessagingContentProvider::class)
        mockkStatic(MmsUtils::class)

        every { DataModel.get() } returns dataModel
        every { dataModel.database } returns database
        every { BugleDatabaseOperations.getThreadId(any(), any()) } returns THREAD_ID
        every {
            BugleDatabaseOperations.getQueryConversationsLatestMessageStatement(any(), any())
        } returns latestMessageStatement
        every { latestMessageStatement.simpleQueryForString() } returns MESSAGE_ID
        every { database.update(any(), any(), any(), any()) } returns 1
        every { MmsUtils.updateSmsReadStatus(any(), any()) } just runs
        every { MessagingContentProvider.notifyMessagesChanged(any()) } just runs
        every { BugleNotifications.cancel(any(), any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun markConversationRead_updatesMessagesAndCancelsNotification() {
        val values = slot<ContentValues>()

        store.markConversationRead(CONVERSATION_ID)

        verify { MmsUtils.updateSmsReadStatus(THREAD_ID, Long.MAX_VALUE) }
        verify {
            database.update(
                DatabaseHelper.MESSAGES_TABLE,
                capture(values),
                any(),
                match { arguments -> arguments.contentEquals(arrayOf(CONVERSATION_ID)) },
            )
        }
        assertEquals(1, values.captured.getAsInteger(DatabaseHelper.MessageColumns.READ))
        assertEquals(1, values.captured.getAsInteger(DatabaseHelper.MessageColumns.SEEN))
        verify { MessagingContentProvider.notifyMessagesChanged(CONVERSATION_ID) }
        verify {
            BugleNotifications.cancel(
                PendingIntentConstants.SMS_NOTIFICATION_ID,
                CONVERSATION_ID,
            )
        }
    }

    @Test
    fun markConversationRead_nothingUpdated_skipsTelephonyAndContentNotifications() {
        every { BugleDatabaseOperations.getThreadId(any(), any()) } returns -1L
        every { database.update(any(), any(), any(), any()) } returns 0

        store.markConversationRead(CONVERSATION_ID)

        verify(exactly = 0) { MmsUtils.updateSmsReadStatus(any(), any()) }
        verify(exactly = 0) { MessagingContentProvider.notifyMessagesChanged(any()) }
        verify {
            BugleNotifications.cancel(
                PendingIntentConstants.SMS_NOTIFICATION_ID,
                CONVERSATION_ID,
            )
        }
    }

    @Test
    fun markConversationUnread_updatesLatestMessage() {
        val values = slot<ContentValues>()

        store.markConversationUnread(CONVERSATION_ID)

        verify {
            BugleDatabaseOperations.getQueryConversationsLatestMessageStatement(
                database,
                CONVERSATION_ID,
            )
        }
        verify {
            database.update(
                DatabaseHelper.MESSAGES_TABLE,
                capture(values),
                any(),
                match { arguments -> arguments.contentEquals(arrayOf(MESSAGE_ID)) },
            )
        }
        assertEquals(0, values.captured.getAsInteger(DatabaseHelper.MessageColumns.READ))
        verify { MessagingContentProvider.notifyMessagesChanged(CONVERSATION_ID) }
    }

    @Test
    fun markConversationUnread_noLatestMessage_doesNothing() {
        every { latestMessageStatement.simpleQueryForString() } returns null

        store.markConversationUnread(CONVERSATION_ID)

        verify(exactly = 0) { database.update(any(), any(), any(), any()) }
        verify(exactly = 0) { MessagingContentProvider.notifyMessagesChanged(any()) }
    }

    private companion object {
        private const val CONVERSATION_ID = "conversation-42"
        private const val MESSAGE_ID = "message-24"
        private const val THREAD_ID = 7L
    }
}
