package com.android.messaging.domain.notification.usecase

import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.database.MatrixCursor
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.createIncomingMessagesTestChannel
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.BuglePrefsKeys
import com.android.messaging.util.NotificationChannelUtil
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.Executors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrateConversationNotificationChannelsTest {

    private lateinit var context: Context
    private lateinit var database: DatabaseWrapper
    private lateinit var migrationDispatcher: ExecutorCoroutineDispatcher
    private lateinit var prefs: FakeBuglePrefs

    @Before
    fun setUp() {
        ShadowNotificationManager.reset()
        context = RuntimeEnvironment.getApplication().applicationContext
        migrationDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        prefs = FakeBuglePrefs()
        val dataModel = mockk<DataModel>(relaxed = true)
        installTestFactory(
            context = context,
            dataModel = dataModel,
            prefs = prefs,
        )
        database = createInMemoryActionSyncTestDatabase(context = context)
        every { dataModel.database } returns database
        createIncomingMessagesTestChannel()
    }

    @After
    fun tearDown() {
        migrationDispatcher.close()
        ShadowNotificationManager.reset()
        FactoryTestAccess.reset()
    }

    @Test
    fun invokeMigratesMutedLegacyConversationToDisabledChannel() {
        val conversationId = seedConversation(
            id = 9801L,
            notificationEnabled = false,
        )

        val didRun = createUseCase().invoke()

        val channel = NotificationChannelUtil.getConversationChannel(conversationId)
        assertTrue(didRun)
        assertTrue(isMigrationMarkedComplete())
        assertEquals(NotificationManager.IMPORTANCE_NONE, channel?.importance)
    }

    @Test
    fun invokeMigratesCustomSoundAndVibrationConversation() {
        val ringtoneString = "content://com.android.messaging.test/migration-ringtone"
        val conversationId = seedConversation(
            id = 9802L,
            notificationSoundUri = ringtoneString,
            notificationVibration = false,
        )

        val didRun = createUseCase().invoke()

        val channel = NotificationChannelUtil.getConversationChannel(conversationId)
        assertTrue(didRun)
        assertTrue(isMigrationMarkedComplete())
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel?.importance)
        assertEquals(ringtoneString, channel?.sound?.toString())
        assertFalse(channel?.shouldVibrate() ?: true)
    }

    @Test
    fun invokeMigratesSilentLegacyRingtoneToSilentChannel() {
        val conversationId = seedConversation(
            id = 9809L,
            notificationSoundUri = "",
        )

        val didRun = createUseCase().invoke()

        val channel = NotificationChannelUtil.getConversationChannel(conversationId)
        assertTrue(didRun)
        assertTrue(isMigrationMarkedComplete())
        assertNull(channel?.sound)
    }

    @Test
    fun invokeRunsMigrationOffCallingThread() {
        val callerThread = Thread.currentThread()
        val conversationId = seedConversation(
            id = 9808L,
            notificationEnabled = false,
        )
        val dataModel = mockk<DataModel>()
        every { dataModel.database } answers {
            assertNotEquals(callerThread, Thread.currentThread())
            database
        }
        installTestFactory(
            context = context,
            dataModel = dataModel,
            prefs = prefs,
        )

        val didRun = createUseCase().invoke()

        val channel = NotificationChannelUtil.getConversationChannel(conversationId)
        assertTrue(didRun)
        assertTrue(isMigrationMarkedComplete())
        assertEquals(NotificationManager.IMPORTANCE_NONE, channel?.importance)
    }

    @Test
    fun invokeDoesNotQueryDatabaseWhenMarkerAlreadySet() {
        markMigrationComplete()
        val dataModel = mockk<DataModel>()
        every { dataModel.database } answers {
            throw AssertionError("Completed migration should not query the database")
        }
        installTestFactory(
            context = context,
            dataModel = dataModel,
            prefs = prefs,
        )

        val didRun = createUseCase().invoke()

        assertFalse(didRun)
        assertTrue(isMigrationMarkedComplete())
    }

    @Test
    fun invokeResolvesCursorColumnsByName() {
        val conversationId = "9807"
        val ringtoneString = "content://com.android.messaging.test/reordered-ringtone"
        val reorderedCursor = MatrixCursor(
            arrayOf(
                ConversationColumns.NOTIFICATION_SOUND_URI,
                ConversationColumns.NOTIFICATION_VIBRATION,
                ConversationColumns._ID,
                ConversationColumns.NOTIFICATION_ENABLED,
                ConversationColumns.NAME,
            ),
        ).apply {
            addRow(
                arrayOf<Any?>(
                    ringtoneString,
                    0,
                    conversationId,
                    1,
                    "Reordered projection",
                ),
            )
        }
        val mockedDatabase = mockk<DatabaseWrapper>()
        every {
            mockedDatabase.query(
                DatabaseHelper.CONVERSATIONS_TABLE,
                any<Array<String>>(),
                any(),
                null,
                null,
                null,
                null,
            )
        } returns reorderedCursor
        installFactoryForDatabase(database = mockedDatabase)

        val didRun = createUseCase().invoke()

        val channel = NotificationChannelUtil.getConversationChannel(conversationId)
        assertTrue(didRun)
        assertTrue(isMigrationMarkedComplete())
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel?.importance)
        assertEquals(ringtoneString, channel?.sound?.toString())
        assertFalse(channel?.shouldVibrate() ?: true)
    }

    @Test
    fun invokeMarksCompleteWithoutCreatingChannelsForDefaultRows() {
        val conversationId = seedConversation(id = 9803L)

        val didRun = createUseCase().invoke()

        assertTrue(didRun)
        assertTrue(isMigrationMarkedComplete())
        assertNull(NotificationChannelUtil.getConversationChannel(conversationId))
    }

    @Test
    fun invokeDoesNotOverwriteExistingChannel() {
        val conversationId = seedConversation(
            id = 9804L,
            notificationEnabled = false,
        )
        val existingChannel = NotificationChannelUtil.createConversationChannelForRuntime(
            conversationId = conversationId,
            conversationTitle = "Existing channel",
        )

        val didRun = createUseCase().invoke()

        val channel = NotificationChannelUtil.getConversationChannel(conversationId)
        assertTrue(didRun)
        assertTrue(isMigrationMarkedComplete())
        assertEquals(existingChannel.importance, channel?.importance)
        assertNotEquals(NotificationManager.IMPORTANCE_NONE, channel?.importance)
    }

    @Test
    fun invokeDoesNothingWhenMarkerAlreadySet() {
        val conversationId = seedConversation(
            id = 9805L,
            notificationEnabled = false,
        )
        markMigrationComplete()

        val didRun = createUseCase().invoke()

        assertFalse(didRun)
        assertTrue(isMigrationMarkedComplete())
        assertNull(NotificationChannelUtil.getConversationChannel(conversationId))
    }

    @Test
    fun invokeDoesNotMarkCompleteWhenQueryFails() {
        database.execSQL("DROP TABLE ${DatabaseHelper.CONVERSATIONS_TABLE}")

        val didRun = createUseCase().invoke()

        assertFalse(didRun)
        assertFalse(isMigrationMarkedComplete())
    }

    @Test
    fun invokePropagatesCancellation() {
        val mockedDatabase = mockk<DatabaseWrapper>()
        every {
            mockedDatabase.query(
                DatabaseHelper.CONVERSATIONS_TABLE,
                any<Array<String>>(),
                any(),
                null,
                null,
                null,
                null,
            )
        } throws CancellationException("Migration cancelled")
        installFactoryForDatabase(database = mockedDatabase)

        assertThrows(CancellationException::class.java) {
            createUseCase().invoke()
        }
        assertFalse(isMigrationMarkedComplete())
    }

    @Test
    fun invokePropagatesUnexpectedRuntimeFailure() {
        val mockedDatabase = mockk<DatabaseWrapper>()
        every {
            mockedDatabase.query(
                DatabaseHelper.CONVERSATIONS_TABLE,
                any<Array<String>>(),
                any(),
                null,
                null,
                null,
                null,
            )
        } throws RuntimeException("Unexpected migration failure")
        installFactoryForDatabase(database = mockedDatabase)

        assertThrows(RuntimeException::class.java) {
            createUseCase().invoke()
        }
        assertFalse(isMigrationMarkedComplete())
    }

    @Test
    fun invokeDoesNotMarkCompleteWhenRequiredColumnIsMissing() {
        val mockedDatabase = mockk<DatabaseWrapper>()
        every {
            mockedDatabase.query(
                DatabaseHelper.CONVERSATIONS_TABLE,
                any<Array<String>>(),
                any(),
                null,
                null,
                null,
                null,
            )
        } returns MatrixCursor(
            arrayOf(
                ConversationColumns._ID,
                ConversationColumns.NAME,
                ConversationColumns.NOTIFICATION_ENABLED,
                ConversationColumns.NOTIFICATION_SOUND_URI,
            ),
        )
        installFactoryForDatabase(database = mockedDatabase)

        val didRun = createUseCase().invoke()

        assertFalse(didRun)
        assertFalse(isMigrationMarkedComplete())
    }

    @Test
    fun runtimeChannelCreationIgnoresUnmigratedLegacyMutedRow() {
        val conversationId = seedConversation(
            id = 9806L,
            notificationEnabled = false,
        )
        markMigrationComplete()

        val channel = NotificationChannelUtil.createConversationChannelForRuntime(
            conversationId = conversationId,
            conversationTitle = "Issue 97 regression",
        )

        assertNotEquals(NotificationManager.IMPORTANCE_NONE, channel.importance)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }

    private fun createUseCase(): MigrateConversationNotificationChannels {
        return MigrateConversationNotificationChannelsImpl(
            messagingDbDispatcher = migrationDispatcher,
        )
    }

    private fun installFactoryForDatabase(database: DatabaseWrapper) {
        installTestFactory(
            context = context,
            dataModel = createDataModel(database = database),
            prefs = prefs,
        )
    }

    private fun createDataModel(database: DatabaseWrapper): DataModel {
        return mockk<DataModel>().also { dataModel ->
            every { dataModel.database } returns database
        }
    }

    private fun seedConversation(
        id: Long,
        name: String = "Conversation $id",
        notificationEnabled: Boolean = true,
        notificationSoundUri: String? = null,
        notificationVibration: Boolean = true,
    ): String {
        val values = ContentValues().apply {
            put(ConversationColumns._ID, id)
            put(ConversationColumns.NAME, name)
            put(ConversationColumns.NOTIFICATION_ENABLED, if (notificationEnabled) 1 else 0)
            put(ConversationColumns.NOTIFICATION_SOUND_URI, notificationSoundUri)
            put(ConversationColumns.NOTIFICATION_VIBRATION, if (notificationVibration) 1 else 0)
        }
        database.insert(DatabaseHelper.CONVERSATIONS_TABLE, null, values)
        return id.toString()
    }

    private fun markMigrationComplete() {
        prefs.putBoolean(BuglePrefsKeys.CONVERSATION_NOTIFICATION_CHANNELS_MIGRATED, true)
    }

    private fun isMigrationMarkedComplete(): Boolean {
        return prefs.getBoolean(
            BuglePrefsKeys.CONVERSATION_NOTIFICATION_CHANNELS_MIGRATED,
            false,
        )
    }
}
