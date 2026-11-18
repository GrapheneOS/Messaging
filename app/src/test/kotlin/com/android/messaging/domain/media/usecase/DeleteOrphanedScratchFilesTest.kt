package com.android.messaging.domain.media.usecase

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.core.content.contentValuesOf
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.PartColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.BuglePrefsKeys
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.io.File
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeleteOrphanedScratchFilesTest {

    private lateinit var context: Context
    private lateinit var dataModel: DataModel
    private lateinit var database: DatabaseWrapper
    private lateinit var messagingDbDispatcher: ExecutorCoroutineDispatcher
    private lateinit var prefs: FakeBuglePrefs

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        messagingDbDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        dataModel = mockk<DataModel>(relaxed = true)
        prefs = FakeBuglePrefs()
        installTestFactory(
            context = context,
            dataModel = dataModel,
            prefs = prefs,
        )
        database = createInMemoryActionSyncTestDatabase(context = context)
        every { dataModel.database } returns database
        MediaScratchFileProvider.getDirectory(context).deleteRecursively()
    }

    @After
    fun tearDown() {
        MediaScratchFileProvider.getDirectory(context).deleteRecursively()
        messagingDbDispatcher.close()
        FactoryTestAccess.reset()
    }

    @Test
    fun invokeDeletesExpiredFilesNothingReferences() {
        val orphanedFile = createScratchFile(age = 2.days).file

        runTest {
            createUseCase().invoke().collect()
        }

        assertFalse(orphanedFile.exists())
    }

    @Test
    fun invokeKeepsFilesOfDraftsAndUnsentMessages() {
        val draftAttachment = createScratchFile(age = 2.days, extension = "jpg")
        val failedAttachment = createScratchFile(age = 2.days, extension = "mp4")
        seedMessage(
            status = MessageData.BUGLE_STATUS_OUTGOING_DRAFT,
            partUri = draftAttachment.uri,
        )
        seedMessage(
            status = MessageData.BUGLE_STATUS_OUTGOING_FAILED,
            partUri = failedAttachment.uri,
        )

        runTest {
            createUseCase().invoke().collect()
        }

        assertTrue(draftAttachment.file.exists())
        assertTrue(failedAttachment.file.exists())
    }

    @Test
    fun invokeKeepsFilesShownAsConversationPreviews() {
        val previewFile = createScratchFile(age = 2.days, extension = "jpg")
        val draftPreviewFile = createScratchFile(age = 2.days, extension = "png")
        seedConversation(
            previewUri = previewFile.uri,
            draftPreviewUri = draftPreviewFile.uri,
        )

        runTest {
            createUseCase().invoke().collect()
        }

        assertTrue(previewFile.file.exists())
        assertTrue(draftPreviewFile.file.exists())
    }

    @Test
    fun invokeKeepsFilesWithinTheGracePeriod() {
        val handedOffFile = createScratchFile(age = 23.hours).file

        runTest {
            createUseCase().invoke().collect()
        }

        assertTrue(handedOffFile.exists())
    }

    @Test
    fun invokeSkipsTheDatabaseWhenNoFileHasExpired() {
        createScratchFile(age = 1.hours)
        val interceptedDatabase = spyk(database)
        every { dataModel.database } returns interceptedDatabase

        runTest {
            createUseCase().invoke().collect()
        }

        verify(exactly = 0) {
            interceptedDatabase.query(any<String>(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun invokeKeepsFilesWhenPartsCannotBeRead() {
        val attachment = createScratchFile(age = 2.days, extension = "jpg")
        seedMessage(
            status = MessageData.BUGLE_STATUS_OUTGOING_FAILED,
            partUri = attachment.uri,
        )
        database.execSQL("DROP TABLE ${DatabaseHelper.PARTS_TABLE}")

        runTest {
            createUseCase().invoke().collect()
        }

        assertTrue(attachment.file.exists())
    }

    @Test
    fun invokeCleansUpAtMostOncePerDay() {
        val firstOrphanedFile = createScratchFile(age = 2.days).file
        runTest {
            createUseCase().invoke().collect()
        }
        val secondOrphanedFile = createScratchFile(age = 2.days).file

        runTest {
            createUseCase().invoke().collect()
        }

        assertFalse(firstOrphanedFile.exists())
        assertTrue(secondOrphanedFile.exists())
    }

    @Test
    fun invokeCleansUpAgainADayAfterTheLastCleanup() {
        recordLastCleanup(timeMillis = System.currentTimeMillis() - 25.hours.inWholeMilliseconds)
        val orphanedFile = createScratchFile(age = 2.days).file

        runTest {
            createUseCase().invoke().collect()
        }

        assertFalse(orphanedFile.exists())
    }

    @Test
    fun invokeCleansUpWhenTheLastCleanupIsInTheFuture() {
        recordLastCleanup(timeMillis = System.currentTimeMillis() + 2.days.inWholeMilliseconds)
        val orphanedFile = createScratchFile(age = 2.days).file

        runTest {
            createUseCase().invoke().collect()
        }

        assertFalse(orphanedFile.exists())
    }

    private fun createUseCase(): DeleteOrphanedScratchFiles {
        return DeleteOrphanedScratchFilesImpl(
            context = context,
            messagingDbDispatcher = messagingDbDispatcher,
        )
    }

    private fun createScratchFile(age: Duration, extension: String? = null): ScratchFile {
        val uri = MediaScratchFileProvider.buildMediaScratchSpaceUri(extension)
        val file = MediaScratchFileProvider.getFileFromUri(uri)
        file.writeText("attachment")
        file.setLastModified(System.currentTimeMillis() - age.inWholeMilliseconds)
        return ScratchFile(uri = uri, file = file)
    }

    private fun recordLastCleanup(timeMillis: Long) {
        prefs.putLong(BuglePrefsKeys.LAST_SCRATCH_FILE_CLEANUP_TIME, timeMillis)
    }

    private fun seedConversation(previewUri: Uri? = null, draftPreviewUri: Uri? = null): Long {
        return insertOrFail(
            table = DatabaseHelper.CONVERSATIONS_TABLE,
            values = contentValuesOf(
                ConversationColumns.NAME to "Conversation",
                ConversationColumns.PREVIEW_URI to previewUri?.toString(),
                ConversationColumns.DRAFT_PREVIEW_URI to draftPreviewUri?.toString(),
            ),
        )
    }

    private fun seedMessage(status: Int, partUri: Uri) {
        val conversationId = seedConversation()
        val participantId = insertOrFail(
            table = DatabaseHelper.PARTICIPANTS_TABLE,
            values = contentValuesOf(
                ParticipantColumns.NORMALIZED_DESTINATION to "+1555123$conversationId",
            ),
        )
        val messageId = insertOrFail(
            table = DatabaseHelper.MESSAGES_TABLE,
            values = contentValuesOf(
                MessageColumns.CONVERSATION_ID to conversationId,
                MessageColumns.SENDER_PARTICIPANT_ID to participantId,
                MessageColumns.SELF_PARTICIPANT_ID to participantId,
                MessageColumns.STATUS to status,
            ),
        )
        insertOrFail(
            table = DatabaseHelper.PARTS_TABLE,
            values = contentValuesOf(
                PartColumns.MESSAGE_ID to messageId,
                PartColumns.CONVERSATION_ID to conversationId,
                PartColumns.CONTENT_URI to partUri.toString(),
                PartColumns.CONTENT_TYPE to "image/jpeg",
            ),
        )
    }

    private fun insertOrFail(table: String, values: ContentValues): Long {
        val rowId = database.insert(table, null, values)
        assertTrue("$table insert failed", rowId >= 0)
        return rowId
    }

    private data class ScratchFile(
        val uri: Uri,
        val file: File,
    )
}
