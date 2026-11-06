package com.android.messaging.datamodel

import android.content.Context
import android.content.pm.ProviderInfo
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.util.ReflectionHelpers

/**
 * The notification list is built in one pass over the unseen messages of every conversation, so
 * nothing a message carries may depend on which conversation was read before it, and no message
 * may be dropped from the pass.
 */
@RunWith(RobolectricTestRunner::class)
class MessageNotificationStateUnseenMessagesTest {

    private lateinit var context: Context
    private lateinit var database: DatabaseWrapper
    private var selfParticipantId = -1L

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        val dataModel = mockk<DataModel>(relaxed = true)
        installTestFactory(context = context, dataModel = dataModel)
        database = createInMemoryActionSyncTestDatabase(context)
        every { dataModel.database } returns database
        // The database is created with one self participant row already in it; reusing it keeps
        // the participants table's (normalized_destination, sub_id) unique constraint satisfied.
        selfParticipantId = existingSelfParticipantId()
        installProvider()
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    /**
     * A message with no parts rows joins to a null part id. Grouping the query by that id folds
     * all of them into one row, so every message but one never reaches the notification.
     */
    @Test
    fun unseenMessages_withoutParts_areAllNotified() {
        val conversationId = insertConversation("Two senders")
        insertMessage(conversationId, insertParticipant("+15550001", "First"), TIMESTAMP_3)
        insertMessage(conversationId, insertParticipant("+15550002", "Second"), TIMESTAMP_2)

        val names = notificationNames().toSet()

        assertEquals(
            "a message with no parts was folded away and never notified",
            setOf("First", "Second"),
            names,
        )
    }

    /**
     * A sender whose number matches no contact has no name. The notification must show that
     * sender's own number, never the name of a conversation that happened to be read first.
     *
     * The leak needs a third conversation: the stale value is whatever conversation was created
     * most recently, so the unknown conversation's second message only sees a foreign name if
     * another conversation is created between its two messages.
     *
     * The assertion compares the full list rather than a set: the name that used to leak, "Not
     * Dave", is also a legitimate name in this fixture, so a set would still match.
     */
    @Test
    fun unseenMessages_fromAnUnknownNumber_doNotBorrowAnotherConversationsName() {
        val contactConversation = insertConversation("Deceased Dave")
        insertMessage(
            contactConversation,
            insertParticipant("+15550001", "Deceased Dave"),
            TIMESTAMP_4,
            withPart = true,
        )
        val unknownConversation = insertConversation("+15559999")
        val unknownSender = insertParticipant("+15559999", fullName = null)
        insertMessage(unknownConversation, unknownSender, TIMESTAMP_3, withPart = true)
        val otherContactConversation = insertConversation("Not Dave")
        insertMessage(
            otherContactConversation,
            insertParticipant("+15550002", "Not Dave"),
            TIMESTAMP_2,
            withPart = true,
        )
        insertMessage(unknownConversation, unknownSender, TIMESTAMP_1, withPart = true)

        val names = notificationNames().sorted()

        assertEquals(
            "a message from an unknown number was labelled with another conversation's contact",
            listOf("+15559999", "+15559999", "Deceased Dave", "Not Dave"),
            names,
        )
    }

    private fun notificationNames(): List<String> {
        return onBackgroundThread {
            MessageNotificationState.getNotificationState()
                ?.mConversationsList
                ?.mConversations
                ?.flatMap { it.mLineInfos }
                ?.map { it.mName }
                .orEmpty()
        }
    }

    /**
     * Building the notification state asserts it runs off the main thread, and those assertions
     * throw rather than log, so the work has to leave the test thread.
     */
    private fun <T> onBackgroundThread(block: () -> T): T {
        val executor = Executors.newSingleThreadExecutor()
        return try {
            executor.submit(Callable { block() }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } finally {
            executor.shutdown()
        }
    }

    /** The real provider, serving [database] instead of the on-disk database. */
    private fun installProvider() {
        val provider = MessagingContentProvider()
        provider.attachInfo(
            context,
            ProviderInfo().apply { authority = MessagingContentProvider.AUTHORITY },
        )
        ReflectionHelpers.setField(provider, "mDatabaseWrapper", database)
        ShadowContentResolver.registerProviderInternal(MessagingContentProvider.AUTHORITY, provider)
    }

    private fun existingSelfParticipantId(): Long {
        database.query(
            DatabaseHelper.PARTICIPANTS_TABLE,
            arrayOf(ParticipantColumns._ID),
            ParticipantColumns.SUB_ID + "=?",
            arrayOf(ParticipantData.DEFAULT_SELF_SUB_ID.toString()),
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        throw AssertionError("the test database has no self participant row")
    }

    private fun insertParticipant(destination: String, fullName: String?): Long {
        return database.insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.NORMALIZED_DESTINATION to destination,
                ParticipantColumns.SEND_DESTINATION to destination,
                ParticipantColumns.DISPLAY_DESTINATION to destination,
                ParticipantColumns.FULL_NAME to fullName,
            ),
        )
    }

    /**
     * A 1:1 conversation: the participant count excludes self, so [ONE_OTHER_PARTICIPANT] is what
     * keeps it out of the group branch that the name leak never touches.
     */
    private fun insertConversation(name: String): Long {
        val conversationId = database.insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.NAME to name,
                ConversationColumns.PARTICIPANT_COUNT to ONE_OTHER_PARTICIPANT,
                ConversationColumns.CURRENT_SELF_ID to selfParticipantId.toString(),
            ),
        )
        database.insert(
            DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ConversationParticipantsColumns.CONVERSATION_ID to conversationId,
                ConversationParticipantsColumns.PARTICIPANT_ID to selfParticipantId,
            ),
        )
        return conversationId
    }

    /**
     * Inserts a message and, when [withPart] is set, the text part a normal sms carries. Without a
     * part the message is one of the rows the null-group defect folds together, which would let
     * the second test fail for that reason rather than for the name it is about.
     */
    private fun insertMessage(
        conversationId: Long,
        senderId: Long,
        timestamp: Long,
        withPart: Boolean = false,
    ): Long {
        val messageId = database.insert(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                MessageColumns.CONVERSATION_ID to conversationId,
                MessageColumns.SENDER_PARTICIPANT_ID to senderId,
                MessageColumns.SELF_PARTICIPANT_ID to selfParticipantId,
                MessageColumns.STATUS to MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                MessageColumns.SEEN to 0,
                MessageColumns.READ to 0,
                MessageColumns.RECEIVED_TIMESTAMP to timestamp,
            ),
        )
        if (withPart) {
            database.insert(
                DatabaseHelper.PARTS_TABLE,
                null,
                contentValuesOf(
                    DatabaseHelper.PartColumns.MESSAGE_ID to messageId,
                    DatabaseHelper.PartColumns.CONVERSATION_ID to conversationId,
                    // Without a text content type ConversationMessageData treats the part as an
                    // attachment and parses content_uri, which a text part does not have.
                    DatabaseHelper.PartColumns.CONTENT_TYPE to "text/plain",
                    DatabaseHelper.PartColumns.TEXT to "message",
                ),
            )
        }
        return messageId
    }

    private companion object {
        private const val TIMEOUT_SECONDS = 30L
        private const val ONE_OTHER_PARTICIPANT = 1

        // Distinct timestamps: the query orders by received_timestamp DESC, and the leak only
        // reproduces if the messages are walked in the intended order.
        private const val TIMESTAMP_4 = 1_780_920_400_000L
        private const val TIMESTAMP_3 = 1_780_920_300_000L
        private const val TIMESTAMP_2 = 1_780_920_200_000L
        private const val TIMESTAMP_1 = 1_780_920_100_000L
    }
}
