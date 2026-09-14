package com.android.messaging.datamodel.data

import android.content.Context
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.NotificationChannelUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The conversation list prefixes a snippet with the name of whoever sent the latest message.
 * These cover the fallback order that name resolves through, end to end: the real
 * `conversation_list_view` SQL, the real projection, and [ConversationListItemData.bind].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConversationListItemDataSnippetSenderTest {

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
    fun snippetSenderName_prefersFirstName() {
        seedConversationWithSender(
            firstName = "Zoë",
            fullName = "Zoë Zimmermann",
            displayDestination = DISPLAY_DESTINATION,
        )

        assertEquals("Zoë", loadConversationListItem().snippetSenderName)
    }

    /**
     * Regression test for BUG-001. The contacts provider yields no given name for CJK names,
     * mononyms, and contacts stored with only a structured family name, so `first_name` is empty
     * while `full_name` holds the name the rest of the row already displays. Falling straight
     * through to the phone number shows a raw number next to the resolved name in the title, and
     * makes TalkBack read a number out for the same contact.
     */
    @Test
    fun snippetSenderName_fallsBackToFullName_whenContactHasNoFirstName() {
        seedConversationWithSender(
            firstName = "",
            fullName = "李明Wang",
            displayDestination = DISPLAY_DESTINATION,
        )

        assertEquals("李明Wang", loadConversationListItem().snippetSenderName)
    }

    @Test
    fun snippetSenderName_fallsBackToDisplayDestination_whenContactHasNoName() {
        seedConversationWithSender(
            firstName = "",
            fullName = "",
            displayDestination = DISPLAY_DESTINATION,
        )

        assertEquals(DISPLAY_DESTINATION, loadConversationListItem().snippetSenderName)
    }

    private fun seedConversationWithSender(
        firstName: String,
        fullName: String,
        displayDestination: String,
    ) {
        val participantId = database.insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.NORMALIZED_DESTINATION to NORMALIZED_DESTINATION,
                ParticipantColumns.SEND_DESTINATION to NORMALIZED_DESTINATION,
                ParticipantColumns.DISPLAY_DESTINATION to displayDestination,
                ParticipantColumns.FULL_NAME to fullName,
                ParticipantColumns.FIRST_NAME to firstName,
            ),
        )
        assertTrue("participant insert failed", participantId >= 0)

        val conversationId = database.insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.NAME to CONVERSATION_NAME,
                ConversationColumns.PARTICIPANT_COUNT to 2,
                ConversationColumns.SNIPPET_TEXT to SNIPPET_TEXT,
            ),
        )
        assertTrue("conversation insert failed", conversationId >= 0)

        val messageId = database.insert(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                MessageColumns.CONVERSATION_ID to conversationId,
                MessageColumns.SENDER_PARTICIPANT_ID to participantId,
                MessageColumns.SELF_PARTICIPANT_ID to participantId,
                MessageColumns.STATUS to MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                MessageColumns.RECEIVED_TIMESTAMP to RECEIVED_TIMESTAMP_MILLIS,
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
        private const val CONVERSATION_NAME = "Zoë, 李明Wang, Bob"
        private const val SNIPPET_TEXT = "对我来说没问题"
        private const val RECEIVED_TIMESTAMP_MILLIS = 1_780_920_000_000L
    }
}
