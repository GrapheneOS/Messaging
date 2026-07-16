package com.android.messaging.datamodel

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.NotificationChannelUtil
import com.android.messaging.util.PendingIntentConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MessageNotificationStateFailedMessagesTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        ShadowNotificationManager.reset()
        context = RuntimeEnvironment.getApplication().applicationContext

        val dataModel = mockk<DataModel>(relaxed = true)
        installTestFactory(context = context, dataModel = dataModel)
        every { dataModel.database } returns createInMemoryActionSyncTestDatabase(context)

        NotificationChannelUtil.onCreate(context)
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @After
    fun tearDown() {
        unmockkAll()
        ShadowNotificationManager.reset()
        FactoryTestAccess.reset()
    }

    @Test
    fun checkFailedMessagesPostsNotificationOnExistingChannel() {
        insertFailedOutgoingMessage()

        MessageNotificationState.checkFailedMessages()

        val shadow = shadowOf(notificationManager)
        val notification = shadow.getNotification(
            BugleNotifications.buildNotificationTag(
                PendingIntentConstants.MSG_SEND_ERROR,
                null,
            ),
            PendingIntentConstants.MSG_SEND_ERROR,
        )
        assertNotNull("failure notification was not posted", notification)
        assertNotNull(
            "failure notification posted without a channel, so the system discards it",
            notification.channelId,
        )
        assertNotNull(
            "failure notification posted on a channel that does not exist, so the" +
                " system discards it",
            notificationManager.getNotificationChannel(notification.channelId),
        )
        assertEquals(NotificationChannelUtil.ALERTS_CHANNEL, notification.channelId)
    }

    @Test
    fun checkFailedMessagesCancelsNotificationOnceMessagesAreSeen() {
        insertFailedOutgoingMessage()
        MessageNotificationState.checkFailedMessages()

        DataModel.get().database.update(
            DatabaseHelper.MESSAGES_TABLE,
            contentValuesOf(DatabaseHelper.MessageColumns.SEEN to 1),
            null,
            null,
        )
        MessageNotificationState.checkFailedMessages()

        val shadow = shadowOf(notificationManager)
        assertEquals(0, shadow.size())
    }

    private fun insertFailedOutgoingMessage() {
        val db = DataModel.get().database

        val participantId = db.insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                DatabaseHelper.ParticipantColumns.NORMALIZED_DESTINATION to RECIPIENT,
            ),
        )
        assertTrue("participant insert failed", participantId >= 0)

        val conversationId = db.insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(DatabaseHelper.ConversationColumns.NAME to CONVERSATION_NAME),
        )
        assertTrue("conversation insert failed", conversationId >= 0)

        val messageId = db.insert(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                DatabaseHelper.MessageColumns.CONVERSATION_ID to conversationId,
                DatabaseHelper.MessageColumns.SENDER_PARTICIPANT_ID to participantId,
                DatabaseHelper.MessageColumns.SELF_PARTICIPANT_ID to participantId,
                DatabaseHelper.MessageColumns.STATUS to MessageData.BUGLE_STATUS_OUTGOING_FAILED,
                DatabaseHelper.MessageColumns.SEEN to 0,
                DatabaseHelper.MessageColumns.READ to 0,
                DatabaseHelper.MessageColumns.RECEIVED_TIMESTAMP to RECEIVED_TIMESTAMP_MILLIS,
                DatabaseHelper.MessageColumns.SENT_TIMESTAMP to SENT_TIMESTAMP_MILLIS,
            ),
        )
        assertTrue("message insert failed", messageId >= 0)
    }

    private companion object {
        private const val RECIPIENT = "+15551230000"
        private const val CONVERSATION_NAME = "Test conversation"
        private const val RECEIVED_TIMESTAMP_MILLIS = 1_780_920_000_000L
        private const val SENT_TIMESTAMP_MILLIS = 1_780_919_999_000L
    }
}
