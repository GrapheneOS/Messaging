package com.android.messaging.datamodel

import android.content.Context
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.installTestFactory
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

@RunWith(RobolectricTestRunner::class)
class BugleDatabaseOperationsDraftVisibilityTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var db: DatabaseWrapper
    private lateinit var selfId: String
    private lateinit var senderId: String
    private lateinit var conversationId: String

    @Before
    fun setUp() {
        installTestFactory(context = context)
        db = createInMemoryActionSyncTestDatabase(context = context)
        selfId = insertParticipant(
            subId = ParticipantData.DEFAULT_SELF_SUB_ID,
            destination = "self",
        )
        senderId = insertParticipant(
            subId = ParticipantData.OTHER_THAN_SELF_SUB_ID,
            destination = "+15550100",
        )
        conversationId = db.insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.CURRENT_SELF_ID to selfId,
                ConversationColumns.PARTICIPANT_COUNT to 1,
            ),
        ).toString()
    }

    @After
    fun tearDown() {
        executor.shutdownNow()
        db.database.close()
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun receivingMessage_keepsExistingDraftVisible() {
        writeDraft(MessageData.createDraftSmsMessage(conversationId, selfId, "unsent reply"))

        receiveMessage(text = "new incoming")

        assertEquals("1", conversationColumn(ConversationColumns.SHOW_DRAFT))
        assertEquals("unsent reply", conversationColumn(ConversationColumns.DRAFT_SNIPPET_TEXT))
        assertEquals("new incoming", conversationColumn(ConversationColumns.SNIPPET_TEXT))
    }

    @Test
    fun receivingMessage_withoutDraft_doesNotShowDraft() {
        receiveMessage(text = "new incoming")

        assertEquals("0", conversationColumn(ConversationColumns.SHOW_DRAFT))
    }

    @Test
    fun clearingDraftForSentMessage_doesNotKeepSentMessageAsDraft() {
        val draft = MessageData.createDraftMmsMessage(conversationId, selfId, "sending now", null)
        writeDraft(draft)

        // InsertNewMessageAction clears the draft while keeping the attachments of the MMS that
        // is being sent.
        onDbThread {
            BugleDatabaseOperations.updateDraftMessageData(
                db,
                conversationId,
                draft,
                BugleDatabaseOperations.UPDATE_MODE_CLEAR_DRAFT,
            )
        }
        receiveMessage(text = "new incoming")

        assertEquals("0", conversationColumn(ConversationColumns.SHOW_DRAFT))
        assertEquals("", conversationColumn(ConversationColumns.DRAFT_SNIPPET_TEXT))
    }

    private fun writeDraft(draft: MessageData) {
        onDbThread {
            BugleDatabaseOperations.updateDraftMessageData(
                db,
                conversationId,
                draft,
                BugleDatabaseOperations.UPDATE_MODE_ADD_DRAFT,
            )
        }
    }

    private fun receiveMessage(text: String) {
        val now = System.currentTimeMillis()
        val message = MessageData.createReceivedSmsMessage(
            null,
            conversationId,
            senderId,
            selfId,
            text,
            null,
            now,
            now,
            false,
            false,
        )
        onDbThread {
            db.beginTransaction()
            try {
                BugleDatabaseOperations.insertNewMessageInTransaction(db, message)
                BugleDatabaseOperations.updateConversationMetadataInTransaction(
                    db,
                    conversationId,
                    message.messageId,
                    now,
                    false,
                    false,
                )
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun onDbThread(block: () -> Unit) {
        executor.submit(Callable { block() }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun insertParticipant(subId: Int, destination: String): String {
        return db.insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.SUB_ID to subId,
                ParticipantColumns.NORMALIZED_DESTINATION to destination,
                ParticipantColumns.SEND_DESTINATION to destination,
            ),
        ).toString()
    }

    private fun conversationColumn(column: String): String? {
        return db.rawQuery(
            "SELECT $column FROM ${DatabaseHelper.CONVERSATIONS_TABLE} " +
                "WHERE ${ConversationColumns._ID}=?",
            arrayOf(conversationId),
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
    }
}
