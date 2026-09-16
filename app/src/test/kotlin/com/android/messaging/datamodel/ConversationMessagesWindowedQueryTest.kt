package com.android.messaging.datamodel

import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.PartColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.testutil.installTestFactory
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class ConversationMessagesWindowedQueryTest {

    private val context get() = RuntimeEnvironment.getApplication().applicationContext

    @Before
    fun setUp() {
        installTestFactory(context = context)
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun windowedQuery_returnsTheNewestRowsOfTheUnboundedQuery() {
        withPopulatedDatabase { conversationId, _ ->
            val unbounded = unboundedMessageTexts(conversationId)

            assertEquals(MESSAGE_COUNT, unbounded.size)
            for (limit in listOf(1, 2, 10, MESSAGE_COUNT - 1)) {
                assertEquals(
                    "window of $limit is not the head of the unbounded query",
                    unbounded.take(limit),
                    windowedMessageTexts(limit, conversationId),
                )
            }
        }
    }

    @Test
    fun windowedQuery_withAWindowLargerThanTheConversation_returnsEverything() {
        withPopulatedDatabase { conversationId, _ ->
            val unbounded = unboundedMessageTexts(conversationId)

            assertEquals(unbounded, windowedMessageTexts(MESSAGE_COUNT, conversationId))
            assertEquals(unbounded, windowedMessageTexts(MESSAGE_COUNT * 10, conversationId))
        }
    }

    /**
     * The draft and the other conversation's messages are newer than everything else, so a window
     * that let either through would put them at the top of the conversation.
     */
    @Test
    fun windowedQuery_excludesDraftsAndOtherConversations() {
        withPopulatedDatabase { conversationId, _ ->
            assertEquals(
                listOf("message 24", "message 23"),
                windowedMessageTexts(limit = 2, conversationId = conversationId),
            )
        }
    }

    /**
     * A message can have no parts at all, since an outgoing mms carrying only a subject never gets
     * one, and the left join gives every one of those the same null part id. Grouped by that id
     * they collapse into a single row, so the window comes back shorter than it is, the caller
     * reads that as the whole conversation and stops asking for more, and the messages that were
     * folded away are not in the window either.
     */
    @Test
    fun windowedQuery_withMessagesWithoutParts_returnsEachOfThem() {
        withPopulatedDatabase(messagesWithoutParts = MESSAGES_WITHOUT_PARTS) { conversationId, _ ->
            val unbounded = unboundedMessageIds(conversationId = conversationId)
            val window = windowedMessageIds(
                limit = WINDOW_OVER_THE_MESSAGES_WITHOUT_PARTS,
                conversationId = conversationId,
            )

            assertEquals(
                "the window is short, so the conversation looks fully loaded",
                WINDOW_OVER_THE_MESSAGES_WITHOUT_PARTS,
                window.size,
            )
            assertEquals(MESSAGE_COUNT + MESSAGES_WITHOUT_PARTS, unbounded.size)
            assertEquals(
                "the window is not the newest messages of the conversation",
                unbounded.take(WINDOW_OVER_THE_MESSAGES_WITHOUT_PARTS),
                window,
            )
        }
    }

    /**
     * Grouping moved from the part id to the message id, and those are the same key only as long
     * as every part joins back to its own message. A message carrying several parts still has to
     * come back as the single row that holds all of them, or every multipart message in the
     * conversation would be torn into one row per part.
     */
    @Test
    fun windowedQuery_withAMultipartMessage_returnsItsPartsInOneRow() {
        withPopulatedDatabase(multipartTexts = MULTIPART_TEXTS) { conversationId, _ ->
            val window = windowedMessageTexts(limit = 2, conversationId = conversationId)

            assertEquals("the parts of one message are one row", 2, window.size)
            assertEquals("message ${MESSAGE_COUNT - 1}", window.last())
            // The projection quotes every part and joins them once a message has more than one,
            // in an order group_concat does not promise.
            assertEquals(
                MULTIPART_TEXTS.map { "'$it'" }.toSet(),
                window.first().split(PARTS_DIVIDER).toSet(),
            )
        }
    }

    /**
     * The single message uri is the other half of not walking the conversation: the repository
     * uses it to refresh one message, and it has to come back alone.
     */
    @Test
    fun messageQuery_returnsOnlyThatMessage() {
        withPopulatedDatabase { conversationId, messageIds ->
            assertEquals(
                listOf("message 7"),
                messageTexts(
                    MessagingContentProvider.buildConversationMessageUri(
                        conversationId,
                        messageIds[7],
                    ),
                    conversationId,
                ),
            )
        }
    }

    private fun withPopulatedDatabase(
        messagesWithoutParts: Int = 0,
        multipartTexts: List<String> = emptyList(),
        block: (String, List<String>) -> Unit,
    ) {
        SQLiteDatabase.create(null).use { db ->
            DatabaseHelper.rebuildTables(db)
            val senderId = db.insertParticipant()
            val conversationId = db.insertConversation()
            val otherConversationId = db.insertConversation()

            val messageIds = List(MESSAGE_COUNT) { index ->
                db.insertMessageWithPart(
                    conversationId = conversationId,
                    senderId = senderId,
                    receivedTimestamp = FIRST_TIMESTAMP + index,
                    text = "message $index",
                )
            }
            // Newer than every message above, so that a window small enough to be worth
            // growing is the one that holds them.
            var timestamp = FIRST_TIMESTAMP + MESSAGE_COUNT
            repeat(messagesWithoutParts) {
                db.insertMessage(
                    conversationId = conversationId,
                    senderId = senderId,
                    receivedTimestamp = timestamp++,
                )
            }
            if (multipartTexts.isNotEmpty()) {
                val multipartMessageId = db.insertMessageWithPart(
                    conversationId = conversationId,
                    senderId = senderId,
                    receivedTimestamp = timestamp++,
                    text = multipartTexts.first(),
                )
                multipartTexts.drop(1).forEach { text ->
                    db.insertPart(
                        messageId = multipartMessageId,
                        conversationId = conversationId,
                        text = text,
                    )
                }
            }
            db.insertMessageWithPart(
                conversationId = conversationId,
                senderId = senderId,
                receivedTimestamp = timestamp++,
                text = "draft",
                status = MessageData.BUGLE_STATUS_OUTGOING_DRAFT,
            )
            db.insertMessageWithPart(
                conversationId = otherConversationId,
                senderId = senderId,
                receivedTimestamp = timestamp,
                text = "other conversation",
            )

            installProvider(db = db)

            block(conversationId, messageIds)
        }
    }

    /** The real provider, serving [db] instead of the on-disk database. */
    private fun installProvider(db: SQLiteDatabase) {
        val provider = MessagingContentProvider()
        provider.attachInfo(
            context,
            ProviderInfo().apply { authority = MessagingContentProvider.AUTHORITY },
        )
        // setDatabaseForTest asserts on a flag that only the instrumentation application sets, so
        // the field it writes is written here instead. Nothing else in the provider is replaced.
        ReflectionHelpers.setField(provider, "mDatabaseWrapper", DatabaseWrapper(context, db))
        ShadowContentResolver.registerProviderInternal(MessagingContentProvider.AUTHORITY, provider)
    }

    private fun windowedMessageTexts(limit: Int, conversationId: String): List<String> {
        return messageTexts(
            MessagingContentProvider.buildConversationMessagesUri(conversationId, limit),
            conversationId,
        )
    }

    private fun unboundedMessageTexts(conversationId: String): List<String> {
        return messageTexts(
            MessagingContentProvider.buildConversationMessagesUri(conversationId),
            conversationId,
        )
    }

    private fun windowedMessageIds(limit: Int, conversationId: String): List<String> {
        return messageRows(
            MessagingContentProvider.buildConversationMessagesUri(conversationId, limit),
            conversationId,
            MessageColumns._ID,
        )
    }

    private fun unboundedMessageIds(conversationId: String): List<String> {
        return messageRows(
            MessagingContentProvider.buildConversationMessagesUri(conversationId),
            conversationId,
            MessageColumns._ID,
        )
    }

    private fun messageTexts(uri: Uri, conversationId: String): List<String> {
        return messageRows(uri, conversationId, column = "parts_texts")
    }

    private fun messageRows(uri: Uri, conversationId: String, column: String): List<String> {
        return checkNotNull(context.contentResolver.query(uri, null, null, null, null))
            .use { cursor ->
                // The window and the message id travel as query parameters, so the cursor still
                // has to watch the bare uri that notifyMessagesChanged posts changes on.
                assertEquals(
                    MessagingContentProvider.buildConversationMessagesUri(conversationId),
                    cursor.notificationUri,
                )

                generateSequence { cursor.takeIf(Cursor::moveToNext) }
                    .map { it.getString(it.getColumnIndexOrThrow(column)) }
                    .toList()
            }
    }

    private fun SQLiteDatabase.insertParticipant(): String {
        return insertOrThrow(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(ParticipantColumns.NORMALIZED_DESTINATION to "+15550001"),
        ).toString()
    }

    private fun SQLiteDatabase.insertConversation(): String {
        return insertOrThrow(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(ConversationColumns.NAME to "Conversation"),
        ).toString()
    }

    private fun SQLiteDatabase.insertMessageWithPart(
        conversationId: String,
        senderId: String,
        receivedTimestamp: Long,
        text: String,
        status: Int = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
    ): String {
        val messageId = insertMessage(
            conversationId = conversationId,
            senderId = senderId,
            receivedTimestamp = receivedTimestamp,
            status = status,
        )
        insertPart(messageId = messageId, conversationId = conversationId, text = text)
        return messageId
    }

    private fun SQLiteDatabase.insertPart(
        messageId: String,
        conversationId: String,
        text: String,
    ) {
        insertOrThrow(
            DatabaseHelper.PARTS_TABLE,
            null,
            contentValuesOf(
                PartColumns.MESSAGE_ID to messageId,
                PartColumns.CONVERSATION_ID to conversationId,
                PartColumns.TEXT to text,
            ),
        )
    }

    /** A message with no rows in the parts table, the way a subject only mms is stored. */
    private fun SQLiteDatabase.insertMessage(
        conversationId: String,
        senderId: String,
        receivedTimestamp: Long,
        status: Int = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
    ): String {
        return insertOrThrow(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                MessageColumns.CONVERSATION_ID to conversationId,
                MessageColumns.SENDER_PARTICIPANT_ID to senderId,
                MessageColumns.RECEIVED_TIMESTAMP to receivedTimestamp,
                MessageColumns.STATUS to status,
            ),
        ).toString()
    }

    private companion object {
        private const val MESSAGE_COUNT = 25
        private const val FIRST_TIMESTAMP = 1_700_000_000_000L

        /** More than one, because a single message without parts is still a single row. */
        private const val MESSAGES_WITHOUT_PARTS = 2

        /** Both of the messages without parts, and enough messages around them to tell. */
        private const val WINDOW_OVER_THE_MESSAGES_WITHOUT_PARTS = 5

        private val MULTIPART_TEXTS = listOf("first part", "second part")
        private const val PARTS_DIVIDER = '|'
    }
}
