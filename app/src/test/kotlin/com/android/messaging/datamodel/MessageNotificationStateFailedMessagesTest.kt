package com.android.messaging.datamodel

import android.app.Notification
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.ContentType
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
        insertFailedMessage()

        checkFailedMessages()

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
        insertFailedMessage()
        checkFailedMessages()

        DataModel.get().database.update(
            DatabaseHelper.MESSAGES_TABLE,
            contentValuesOf(DatabaseHelper.MessageColumns.SEEN to 1),
            null,
            null,
        )
        checkFailedMessages()

        val shadow = shadowOf(notificationManager)
        assertEquals(0, shadow.size())
    }

    @Test
    fun checkFailedMessagesShowsTheTextOfTheMessageThatFailed() {
        insertFailedMessage()

        checkFailedMessages()

        assertEquals(
            "the failure notification does not say which message failed",
            MESSAGE_TEXT,
            postedFailureNotification().extras.getCharSequence(Notification.EXTRA_TEXT).toString(),
        )
    }

    @Test
    fun checkFailedMessagesNamesTheConversationTheMessageWasFor() {
        insertFailedMessage()

        checkFailedMessages()

        assertEquals(
            "the failure notification does not say which conversation failed",
            CONVERSATION_NAME,
            postedFailureNotification().extras
                .getCharSequence(Notification.EXTRA_SUB_TEXT).toString(),
        )
    }

    @Test
    fun checkFailedMessagesStampsTheNotificationWithTheTimeOfTheFailedMessage() {
        insertFailedMessage()

        checkFailedMessages()

        assertEquals(
            "the failure notification is stamped with the time of the check, not the message",
            RECEIVED_TIMESTAMP_MILLIS,
            postedFailureNotification().`when`,
        )
    }

    @Test
    fun checkFailedMessagesOffersToSendTheFailedMessageAgain() {
        insertFailedMessage()

        checkFailedMessages()

        val actions = postedFailureNotification().actions
        assertNotNull("the failure notification offers no way to retry", actions)
        assertEquals("expected exactly one action on the failure notification", 1, actions.size)
        assertEquals(
            context.getString(R.string.notification_retry_prompt),
            actions.single().title.toString(),
        )
    }

    @Test
    fun checkFailedMessagesDescribesTheAttachmentWhenTheMessageHasNoText() {
        insertFailedMessage(
            partValues = contentValuesOf(
                DatabaseHelper.PartColumns.CONTENT_URI to "content://mms/part/1",
                DatabaseHelper.PartColumns.CONTENT_TYPE to ContentType.IMAGE_PNG,
            ),
        )

        checkFailedMessages()

        assertEquals(
            "an attachment-only message leaves the failure notification blank",
            context.getString(R.string.notification_picture),
            postedFailureNotification().extras.getCharSequence(Notification.EXTRA_TEXT).toString(),
        )
    }

    @Test
    fun checkFailedMessagesOffersToDownloadAgainWhenTheDownloadFailed() {
        insertFailedMessage(status = MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED)

        checkFailedMessages()

        val actions = postedFailureNotification().actions
        assertNotNull("the failure notification offers no way to retry the download", actions)
        assertEquals(
            "a failed download must offer to download again, not to send again",
            context.getString(R.string.notification_download_mms),
            actions.single().title.toString(),
        )
    }

    @Test
    fun checkFailedMessagesFallsBackToTheSentTimeWhenNothingWasReceived() {
        insertFailedMessage(receivedTimestamp = 0L)

        checkFailedMessages()

        assertEquals(
            "a message with no received time leaves the notification stamped with the check",
            SENT_TIMESTAMP_MILLIS,
            postedFailureNotification().`when`,
        )
    }

    private fun checkFailedMessages() {
        var failure: Throwable? = null
        val thread = Thread(
            { MessageNotificationState.checkFailedMessages() },
            "notification-check",
        )
        thread.setUncaughtExceptionHandler { _, throwable -> failure = throwable }
        thread.start()
        thread.join()
        failure?.let { throw it }
    }

    private fun postedFailureNotification(): Notification {
        val notification = shadowOf(notificationManager).getNotification(
            BugleNotifications.buildNotificationTag(
                PendingIntentConstants.MSG_SEND_ERROR,
                null,
            ),
            PendingIntentConstants.MSG_SEND_ERROR,
        )
        assertNotNull("failure notification was not posted", notification)
        return notification
    }

    private fun insertFailedMessage(
        partValues: ContentValues = contentValuesOf(
            DatabaseHelper.PartColumns.TEXT to MESSAGE_TEXT,
            DatabaseHelper.PartColumns.CONTENT_TYPE to ContentType.TEXT_PLAIN,
        ),
        status: Int = MessageData.BUGLE_STATUS_OUTGOING_FAILED,
        receivedTimestamp: Long = RECEIVED_TIMESTAMP_MILLIS,
    ) {
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
                DatabaseHelper.MessageColumns.STATUS to status,
                DatabaseHelper.MessageColumns.SEEN to 0,
                DatabaseHelper.MessageColumns.READ to 0,
                DatabaseHelper.MessageColumns.RECEIVED_TIMESTAMP to receivedTimestamp,
                DatabaseHelper.MessageColumns.SENT_TIMESTAMP to SENT_TIMESTAMP_MILLIS,
            ),
        )
        assertTrue("message insert failed", messageId >= 0)

        partValues.put(DatabaseHelper.PartColumns.MESSAGE_ID, messageId)
        partValues.put(DatabaseHelper.PartColumns.CONVERSATION_ID, conversationId)
        val partId = db.insert(DatabaseHelper.PARTS_TABLE, null, partValues)
        assertTrue("part insert failed", partId >= 0)
    }

    private companion object {
        private const val RECIPIENT = "+15551230000"
        private const val CONVERSATION_NAME = "Test conversation"
        private const val MESSAGE_TEXT = "the message that failed to send"
        private const val RECEIVED_TIMESTAMP_MILLIS = 1_780_920_000_000L
        private const val SENT_TIMESTAMP_MILLIS = 1_780_919_999_000L
    }
}
