package com.android.messaging.data.search.repository

import android.content.Context
import android.content.pm.ProviderInfo
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.PartColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.testutil.installTestFactory
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.util.ReflectionHelpers

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MessageSearchRepositoryImplTest {

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
    fun search_mapsMatchingMessagesToResults() = runTest {
        val db = SQLiteDatabase.create(null)
        try {
            DatabaseHelper.rebuildTables(db)
            val senderId = db.insertParticipant()
            val conversationId = db.insertConversation()
            val messageId = db.insertMessageWithPart(conversationId, senderId, 1_000L, "needle")
            installProvider(db)

            val repository = repository(testScheduler)

            val results = repository.search("needle").first()

            assertEquals(1, results.size)
            val result = results.single()
            assertEquals(MessageId(messageId), result.messageId)
            assertEquals(ConversationId(conversationId), result.conversationId)
            assertEquals("needle", result.snippet)
            assertEquals(1_000L, result.timestamp)
            assertEquals("Conversation", result.conversationName)
            assertEquals("Sam", result.senderName)
        } finally {
            db.close()
        }
    }

    @Test
    fun search_blankQuery_returnsEmpty() = runTest {
        val results = repository(testScheduler).search("   ").first()

        assertTrue(results.isEmpty())
    }

    private fun repository(scheduler: TestCoroutineScheduler): MessageSearchRepositoryImpl {
        return MessageSearchRepositoryImpl(
            contentResolver = context.contentResolver,
            messagingDbDispatcher = UnconfinedTestDispatcher(scheduler),
        )
    }

    private fun installProvider(db: SQLiteDatabase) {
        val provider = MessagingContentProvider()
        provider.attachInfo(
            context,
            ProviderInfo().apply { authority = MessagingContentProvider.AUTHORITY },
        )
        ReflectionHelpers.setField(provider, "mDatabaseWrapper", databaseWrapper(db))
        ShadowContentResolver.registerProviderInternal(MessagingContentProvider.AUTHORITY, provider)
    }

    private fun databaseWrapper(db: SQLiteDatabase): DatabaseWrapper {
        val constructor = DatabaseWrapper::class.java.getDeclaredConstructor(
            Context::class.java,
            SQLiteDatabase::class.java,
        )
        constructor.isAccessible = true
        return constructor.newInstance(context, db)
    }

    private fun SQLiteDatabase.insertParticipant(): String {
        return insertOrThrow(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.NORMALIZED_DESTINATION to "+15550001",
                ParticipantColumns.FULL_NAME to "Sam",
            ),
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
    ): String {
        val messageId = insertOrThrow(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                MessageColumns.CONVERSATION_ID to conversationId,
                MessageColumns.SENDER_PARTICIPANT_ID to senderId,
                MessageColumns.RECEIVED_TIMESTAMP to receivedTimestamp,
                MessageColumns.STATUS to MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            ),
        ).toString()
        insertOrThrow(
            DatabaseHelper.PARTS_TABLE,
            null,
            contentValuesOf(
                PartColumns.MESSAGE_ID to messageId,
                PartColumns.CONVERSATION_ID to conversationId,
                PartColumns.TEXT to text,
                PartColumns.CONTENT_TYPE to "text/plain",
            ),
        )
        return messageId
    }
}
