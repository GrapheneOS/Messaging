package com.android.messaging.datamodel.data

import android.content.Context
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.NotificationChannelUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The conversation list derives the unread indicator from the read flag of the newest message.
 * `conversation_list_view` reaches that flag through a LEFT JOIN on `latest_message_id`, which no
 * message in a conversation that only holds a draft ever sets, so the column comes back NULL.
 * These cover that end to end: the real view SQL, the real projection, and
 * [ConversationListItemData.bind].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConversationListItemDataReadStateTest {

    private lateinit var context: Context
    private lateinit var database: DatabaseWrapper

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext

        val dataModel = mockk<DataModel>(relaxed = true)
        installTestFactory(context = context, dataModel = dataModel)
        database = createInMemoryActionSyncTestDatabase(context)
        every { dataModel.database } returns database

        NotificationChannelUtil.onCreate(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun isRead_whenConversationHasOnlyADraft() {
        seedConversationWithDraft()

        val item = loadConversationListItem()

        assertTrue("a conversation with no received message must not show as unread", item.isRead)
    }

    @Test
    fun isRead_whenConversationHasAnUnreadMessage() {
        seedConversationWithMessage(isRead = false)

        assertFalse(loadConversationListItem().isRead)
    }

    @Test
    fun isRead_whenConversationHasAReadMessage() {
        seedConversationWithMessage(isRead = true)

        assertTrue(loadConversationListItem().isRead)
    }

    private fun seedConversationWithDraft() {
        val conversationId = insertConversation(withDraft = true)
        assertTrue("conversation insert failed", conversationId >= 0)
    }

    private fun seedConversationWithMessage(isRead: Boolean) {
        val participantId = database.insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.NORMALIZED_DESTINATION to NORMALIZED_DESTINATION,
                ParticipantColumns.SEND_DESTINATION to NORMALIZED_DESTINATION,
                ParticipantColumns.DISPLAY_DESTINATION to DISPLAY_DESTINATION,
            ),
        )
        assertTrue("participant insert failed", participantId >= 0)

        val conversationId = insertConversation(withDraft = false)
        assertTrue("conversation insert failed", conversationId >= 0)

        val messageId = database.insert(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                DatabaseHelper.MessageColumns.CONVERSATION_ID to conversationId,
                DatabaseHelper.MessageColumns.SENDER_PARTICIPANT_ID to participantId,
                DatabaseHelper.MessageColumns.SELF_PARTICIPANT_ID to participantId,
                DatabaseHelper.MessageColumns.STATUS to MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                DatabaseHelper.MessageColumns.READ to if (isRead) 1 else 0,
                DatabaseHelper.MessageColumns.RECEIVED_TIMESTAMP to RECEIVED_TIMESTAMP_MILLIS,
            ),
        )
        assertTrue("message insert failed", messageId >= 0)

        database.update(
            DatabaseHelper.CONVERSATIONS_TABLE,
            contentValuesOf(ConversationColumns.LATEST_MESSAGE_ID to messageId),
            "${ConversationColumns._ID}=?",
            arrayOf(conversationId.toString()),
        )
    }

    private fun insertConversation(withDraft: Boolean): Long {
        return database.insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.NAME to CONVERSATION_NAME,
                ConversationColumns.PARTICIPANT_COUNT to 2,
                ConversationColumns.SHOW_DRAFT to if (withDraft) 1 else 0,
                ConversationColumns.DRAFT_SNIPPET_TEXT to if (withDraft) DRAFT_TEXT else "",
            ),
        )
    }

    private fun loadConversationListItem(): ConversationListItemData {
        return database.query(
            ConversationListItemData.getConversationListView(),
            ConversationListItemData.PROJECTION,
            null,
            null,
            null,
            null,
            null,
        ).use { cursor ->
            assertTrue("conversation_list_view returned no rows", cursor.moveToFirst())
            ConversationListItemData().apply { bind(cursor) }
        }
    }

    private companion object {
        private const val NORMALIZED_DESTINATION = "+37255500004"
        private const val DISPLAY_DESTINATION = "+372 5550 0004"
        private const val CONVERSATION_NAME = "Bob"
        private const val DRAFT_TEXT = "half-typed message"
        private const val RECEIVED_TIMESTAMP_MILLIS = 1_780_920_000_000L
    }
}
