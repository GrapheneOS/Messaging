package com.android.messaging.domain.notification.usecase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.database.CursorWrapper
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.createIncomingMessagesTestChannel
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.NotificationChannelUtil
import com.android.messaging.util.db.ext.withTransaction
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeleteUnlistedConversationChannelsTest {

    private lateinit var context: Context
    private lateinit var dataModel: DataModel
    private lateinit var database: DatabaseWrapper
    private lateinit var messagingDbDispatcher: ExecutorCoroutineDispatcher

    @Before
    fun setUp() {
        ShadowNotificationManager.reset()
        context = RuntimeEnvironment.getApplication().applicationContext
        messagingDbDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        dataModel = mockk<DataModel>(relaxed = true)
        installTestFactory(
            context = context,
            dataModel = dataModel,
            prefs = FakeBuglePrefs(),
        )
        database = createInMemoryActionSyncTestDatabase(context = context)
        every { dataModel.database } returns database
        createIncomingMessagesTestChannel()
    }

    @After
    fun tearDown() {
        messagingDbDispatcher.close()
        ShadowNotificationManager.reset()
        FactoryTestAccess.reset()
    }

    @Test
    fun invokeDeletesChannelsOfConversationsHiddenFromTheList() {
        val emptyConversationId = seedConversation(id = 9901L, sortTimestamp = 0L)
        createConversationChannel(conversationId = emptyConversationId)

        runTest {
            createUseCase().invoke().collect()
        }

        assertNull(NotificationChannelUtil.getConversationChannel(emptyConversationId))
        assertNotNull(
            NotificationChannelUtil
                .getNotificationManager()
                .getNotificationChannel(NotificationChannelUtil.INCOMING_MESSAGES),
        )
    }

    @Test
    fun invokeDeletesChannelsOfDeletedConversations() {
        val deletedConversationId = "9902"
        createConversationChannel(conversationId = deletedConversationId)

        runTest {
            createUseCase().invoke().collect()
        }

        assertNull(NotificationChannelUtil.getConversationChannel(deletedConversationId))
    }

    @Test
    fun invokeKeepsChannelsOfListedConversations() {
        val listedConversationId = seedConversation(id = 9903L, sortTimestamp = 1_000L)
        createConversationChannel(conversationId = listedConversationId)

        runTest {
            createUseCase().invoke().collect()
        }

        assertNotNull(NotificationChannelUtil.getConversationChannel(listedConversationId))
    }

    @Test
    fun invokeKeepsChannelsOutsideTheConversationsChannel() {
        val channelId = "9904"
        val notificationManager = NotificationChannelUtil.getNotificationManager()
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, "Other", NotificationManager.IMPORTANCE_DEFAULT),
        )

        runTest {
            createUseCase().invoke().collect()
        }

        assertNotNull(notificationManager.getNotificationChannel(channelId))
    }

    @Test
    fun invokeKeepsChannelsWhenConversationsCannotBeRead() {
        val conversationId = seedConversation(id = 9905L, sortTimestamp = 1_000L)
        createConversationChannel(conversationId = conversationId)
        database.execSQL("DROP TABLE ${DatabaseHelper.CONVERSATIONS_TABLE}")

        runTest {
            createUseCase().invoke().collect()
        }

        assertNotNull(NotificationChannelUtil.getConversationChannel(conversationId))
    }

    @Test
    fun invokeKeepsChannelsOfConversationsListedWhileItRuns() {
        val conversationId = seedConversation(id = 9906L, sortTimestamp = 0L)
        createConversationChannel(conversationId = conversationId)
        val incomingMessage = thread(start = false) {
            database.withTransaction {
                database.update(
                    DatabaseHelper.CONVERSATIONS_TABLE,
                    ContentValues().apply { put(ConversationColumns.SORT_TIMESTAMP, 2_000L) },
                    "${ConversationColumns._ID} = ?",
                    arrayOf(conversationId),
                )
            }
            // The incoming message's notification reuses the conversation's channel
            createConversationChannel(conversationId = conversationId)
        }
        receiveAfterListedConversationsAreRead(incomingMessage = incomingMessage)

        runTest {
            createUseCase().invoke().collect()
        }
        incomingMessage.join(5_000L)

        assertNotNull(NotificationChannelUtil.getConversationChannel(conversationId))
    }

    private fun receiveAfterListedConversationsAreRead(incomingMessage: Thread) {
        val interceptedDatabase = spyk(database)
        every { dataModel.database } returns interceptedDatabase
        every {
            interceptedDatabase.query(
                DatabaseHelper.CONVERSATIONS_TABLE,
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } answers {
            object : CursorWrapper(callOriginal()) {
                override fun close() {
                    super.close()
                    incomingMessage.start()
                    // The message is saved right away unless the database is held for the delete
                    while (incomingMessage.state in RUNNING_THREAD_STATES) {
                        Thread.yield()
                    }
                }
            }
        }
    }

    private fun createUseCase(): DeleteUnlistedConversationChannels {
        return DeleteUnlistedConversationChannelsImpl(
            messagingDbDispatcher = messagingDbDispatcher,
        )
    }

    private fun seedConversation(id: Long, sortTimestamp: Long): String {
        val values = ContentValues().apply {
            put(ConversationColumns._ID, id)
            put(ConversationColumns.NAME, "Conversation $id")
            put(ConversationColumns.SORT_TIMESTAMP, sortTimestamp)
        }
        database.insert(DatabaseHelper.CONVERSATIONS_TABLE, null, values)
        return id.toString()
    }

    private fun createConversationChannel(conversationId: String) {
        NotificationChannelUtil.createConversationChannelForRuntime(
            conversationId = conversationId,
            conversationTitle = "Conversation $conversationId",
        )
    }

    private companion object {
        private val RUNNING_THREAD_STATES = setOf(Thread.State.RUNNABLE, Thread.State.BLOCKED)
    }
}
