package com.android.messaging.data.conversation.repository

import android.content.Context
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapperImpl
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapperImpl
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.store.ConversationDraftStore
import com.android.messaging.data.conversation.store.ConversationDraftStoreImpl
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Adding people to a conversation opens a different (group) conversation, so whatever was being
 * composed (typically a shared photo or link) has to travel with it. These run the real draft SQL
 * so the scratch files backing the moved attachments are covered too.
 */
@RunWith(RobolectricTestRunner::class)
class ConversationDraftsRepositoryMoveDraftTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var database: DatabaseWrapper
    private lateinit var repository: ConversationDraftsRepository
    private var sourceConversationId = ConversationId(value = "")
    private var targetConversationId = ConversationId(value = "")
    private lateinit var sharedImageUri: String
    private lateinit var targetImageUri: String

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext

        val dataModel = mockk<DataModel>(relaxed = true)
        installTestFactory(context = context, dataModel = dataModel)
        database = createInMemoryActionSyncTestDatabase(context)
        every { dataModel.database } returns database

        // Draft loading drops attachments whose file can't be opened, so back them with real files
        Robolectric.setupContentProvider(
            MediaScratchFileProvider::class.java,
            MediaScratchFileProvider.AUTHORITY,
        )
        sharedImageUri = MediaScratchFileProvider.buildMediaScratchSpaceUri("png").toString()
        targetImageUri = MediaScratchFileProvider.buildMediaScratchSpaceUri("png").toString()

        val selfParticipantId = insertSelfParticipant()
        sourceConversationId = insertConversation(selfParticipantId = selfParticipantId)
        targetConversationId = insertConversation(selfParticipantId = selfParticipantId)

        repository = createRepository(conversationDraftStore = ConversationDraftStoreImpl())
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun moveDraft_movesSharedAttachmentToTargetConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            repository.saveDraft(
                conversationId = sourceConversationId,
                draft = draft(messageText = "look", attachmentUri = sharedImageUri),
            )

            repository.moveDraft(
                fromConversationId = sourceConversationId,
                toConversationId = targetConversationId,
            )

            val targetDraft = loadDraft(conversationId = targetConversationId)
            assertEquals("look", targetDraft.messageText)
            assertEquals(listOf(sharedImageUri), targetDraft.attachments.map { it.contentUri })
            assertEquals("1", readConversationColumn(targetConversationId, SHOW_DRAFT))
            assertEquals(
                sharedImageUri,
                readConversationColumn(targetConversationId, DRAFT_PREVIEW_URI),
            )
        }
    }

    @Test
    fun moveDraft_clearsSourceConversationWithoutDeletingMovedAttachmentFile() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            repository.saveDraft(
                conversationId = sourceConversationId,
                draft = draft(messageText = "look", attachmentUri = sharedImageUri),
            )

            repository.moveDraft(
                fromConversationId = sourceConversationId,
                toConversationId = targetConversationId,
            )

            assertFalse(loadDraft(conversationId = sourceConversationId).hasContent)
            assertEquals("0", readConversationColumn(sourceConversationId, SHOW_DRAFT))
            assertEquals("", readConversationColumn(sourceConversationId, DRAFT_PREVIEW_URI))
            assertTrue(scratchFileExists(uri = sharedImageUri))
        }
    }

    @Test
    fun moveDraft_keepsDraftAlreadyInTargetConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            repository.saveDraft(
                conversationId = targetConversationId,
                draft = draft(messageText = "hi all", attachmentUri = targetImageUri),
            )
            repository.saveDraft(
                conversationId = sourceConversationId,
                draft = draft(messageText = "look", attachmentUri = sharedImageUri),
            )

            repository.moveDraft(
                fromConversationId = sourceConversationId,
                toConversationId = targetConversationId,
            )

            val targetDraft = loadDraft(conversationId = targetConversationId)
            assertEquals("hi all\nlook", targetDraft.messageText)
            assertEquals(
                listOf(targetImageUri, sharedImageUri),
                targetDraft.attachments.map { it.contentUri },
            )
        }
    }

    @Test
    fun moveDraft_toSameConversation_keepsDraftUnchanged() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            repository.saveDraft(
                conversationId = sourceConversationId,
                draft = draft(messageText = "look", attachmentUri = sharedImageUri),
            )

            repository.moveDraft(
                fromConversationId = sourceConversationId,
                toConversationId = sourceConversationId,
            )

            val sourceDraft = loadDraft(conversationId = sourceConversationId)
            assertEquals("look", sourceDraft.messageText)
            assertEquals(listOf(sharedImageUri), sourceDraft.attachments.map { it.contentUri })
        }
    }

    @Test
    fun moveDraft_keepsBothSubjectsWhenTheyDiffer() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            repository.saveDraft(
                conversationId = targetConversationId,
                draft = ConversationDraft(subjectText = "Dinner"),
            )
            repository.saveDraft(
                conversationId = sourceConversationId,
                draft = ConversationDraft(subjectText = "Photos"),
            )

            repository.moveDraft(
                fromConversationId = sourceConversationId,
                toConversationId = targetConversationId,
            )

            assertEquals(
                "Dinner Photos",
                loadDraft(conversationId = targetConversationId).subjectText,
            )
        }
    }

    @Test
    fun moveDraft_whenDraftIsNotWritten_failsAndKeepsSourceDraft() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            repository.saveDraft(
                conversationId = sourceConversationId,
                draft = draft(messageText = "look", attachmentUri = sharedImageUri),
            )
            val conversationDraftStore = ConversationDraftStoreImpl()
            // The target conversation disappears after the drafts are read, right before the write
            val targetDeletingStore = object : ConversationDraftStore by conversationDraftStore {
                override fun moveDraftMessage(
                    fromConversationId: ConversationId,
                    toConversationId: ConversationId,
                    message: MessageData,
                ) {
                    database.delete(
                        DatabaseHelper.CONVERSATIONS_TABLE,
                        "${ConversationColumns._ID}=?",
                        arrayOf(toConversationId.value),
                    )
                    conversationDraftStore.moveDraftMessage(
                        fromConversationId = fromConversationId,
                        toConversationId = toConversationId,
                        message = message,
                    )
                }
            }

            val failure = runCatching {
                createRepository(conversationDraftStore = targetDeletingStore).moveDraft(
                    fromConversationId = sourceConversationId,
                    toConversationId = targetConversationId,
                )
            }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
            val sourceDraft = loadDraft(conversationId = sourceConversationId)
            assertEquals("look", sourceDraft.messageText)
            assertEquals(listOf(sharedImageUri), sourceDraft.attachments.map { it.contentUri })
        }
    }

    private fun createRepository(
        conversationDraftStore: ConversationDraftStore,
    ): ConversationDraftsRepository {
        return ConversationDraftsRepositoryImpl(
            contentResolver = context.contentResolver,
            conversationDraftMessageDataMapper = ConversationDraftMessageDataMapperImpl(),
            conversationMessageDataDraftMapper = ConversationMessageDataDraftMapperImpl(),
            conversationDraftStore = conversationDraftStore,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            messagingDbDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private suspend fun loadDraft(conversationId: ConversationId): ConversationDraft {
        return repository.observeConversationDraft(conversationId = conversationId).first()
    }

    private fun scratchFileExists(uri: String): Boolean {
        return MediaScratchFileProvider.getFileFromUri(uri.toUri()).exists()
    }

    private fun draft(messageText: String, attachmentUri: String): ConversationDraft {
        return ConversationDraft(
            messageText = messageText,
            attachments = persistentListOf(
                ConversationDraftAttachment(
                    contentType = "image/png",
                    contentUri = attachmentUri,
                ),
            ),
        )
    }

    private fun insertSelfParticipant(): Long {
        return database.insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.SUB_ID to ParticipantData.DEFAULT_SELF_SUB_ID,
                ParticipantColumns.NORMALIZED_DESTINATION to "self",
                ParticipantColumns.SEND_DESTINATION to "self",
            ),
        )
    }

    private fun insertConversation(selfParticipantId: Long): ConversationId {
        val conversationId = database.insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.CURRENT_SELF_ID to selfParticipantId,
                ConversationColumns.PARTICIPANT_COUNT to 1,
            ),
        )

        return ConversationId(conversationId.toString())
    }

    private fun readConversationColumn(conversationId: ConversationId, column: String): String {
        return database.query(
            DatabaseHelper.CONVERSATIONS_TABLE,
            arrayOf(column),
            "${ConversationColumns._ID}=?",
            arrayOf(conversationId.value),
            null,
            null,
            null,
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }
    }

    private companion object {
        private const val SHOW_DRAFT = ConversationColumns.SHOW_DRAFT
        private const val DRAFT_PREVIEW_URI = ConversationColumns.DRAFT_PREVIEW_URI
    }
}
