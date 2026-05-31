package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.media.MediaMetadataRetriever
import android.net.Uri
import app.cash.turbine.test
import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapperImpl
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapperImpl
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.store.ConversationDraftStore
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.util.MediaMetadataRetrieverWrapper
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationDraftsRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var contentResolver: ContentResolver
    private lateinit var conversationDraftStore: ConversationDraftStore

    @Before
    fun setUp() {
        contentResolver = mockk()
        conversationDraftStore = mockk()
        mockkStatic(MessagingContentProvider::class)
        every {
            MessagingContentProvider.buildConversationMetadataUri(any())
        } answers {
            Uri.parse("content://conversation/${firstArg<String>()}/metadata")
        }
        every {
            MessagingContentProvider.notifyConversationMetadataChanged(any())
        } just runs
    }

    @After
    fun tearDown() {
        unmockkConstructor(MediaMetadataRetrieverWrapper::class)
        unmockkAll()
    }

    @Test
    fun observeConversationDraft_registersAndUnregistersObserverForCollection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider
                .buildConversationMetadataUri(CONVERSATION_ID)
            val repository = createRepository()

            every {
                conversationDraftStore.getSelfParticipantId(CONVERSATION_ID)
            } returns "self-1"
            every {
                conversationDraftStore.readDraftMessage(
                    conversationId = CONVERSATION_ID,
                    selfParticipantId = "self-1",
                )
            } returns MessageData.createDraftSmsMessage(
                CONVERSATION_ID,
                "self-1",
                "Hello",
            )
            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )

            repository.observeConversationDraft(conversationId = CONVERSATION_ID).test {
                assertEquals("Hello", awaitItem().messageText)
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                contentResolver.registerContentObserver(
                    expectedUri,
                    true,
                    registeredObserver.captured,
                )
            }
            verify(exactly = 1) {
                contentResolver.unregisterContentObserver(registeredObserver.captured)
            }
        }
    }

    @Test
    fun observeConversationDraft_reloadsDraftWhenObserverChanges() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)
            val repository = createRepository()
            var currentDraftMessage: MessageData? = MessageData.createDraftSmsMessage(
                CONVERSATION_ID,
                "self-1",
                "Before",
            )

            every {
                conversationDraftStore.getSelfParticipantId(CONVERSATION_ID)
            } returns "self-1"
            every {
                conversationDraftStore.readDraftMessage(
                    conversationId = CONVERSATION_ID,
                    selfParticipantId = "self-1",
                )
            } answers {
                currentDraftMessage
            }
            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )

            repository.observeConversationDraft(conversationId = CONVERSATION_ID).test {
                assertEquals("Before", awaitItem().messageText)

                currentDraftMessage = MessageData.createDraftMmsMessage(
                    CONVERSATION_ID,
                    "self-1",
                    "",
                    "Updated subject",
                )
                registeredObserver.captured.onChange(false)

                val updatedDraft = awaitItem()
                assertEquals("Updated subject", updatedDraft.subjectText)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeConversationDraft_emitsEmptyDraftWhenConversationDoesNotExist() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)
            val repository = createRepository()

            every {
                conversationDraftStore.getSelfParticipantId(CONVERSATION_ID)
            } returns null
            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )

            repository.observeConversationDraft(conversationId = CONVERSATION_ID).test {
                assertEquals(ConversationDraft(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeConversationDraft_emitsSafeEmptyDraftWhenLoadingFails() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)
            val repository = createRepository()

            every {
                conversationDraftStore.getSelfParticipantId(CONVERSATION_ID)
            } throws IllegalStateException("boom")
            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )

            repository.observeConversationDraft(conversationId = CONVERSATION_ID).test {
                assertEquals(ConversationDraft(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeConversationDraft_resolvesAudioAttachmentDuration() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)
            val repository = createRepository()

            mockkConstructor(MediaMetadataRetrieverWrapper::class)
            every {
                anyConstructed<MediaMetadataRetrieverWrapper>().setDataSource(any())
            } just runs
            every {
                anyConstructed<MediaMetadataRetrieverWrapper>().extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION,
                )
            } returns "3210"
            every {
                anyConstructed<MediaMetadataRetrieverWrapper>().release()
            } just runs

            every {
                conversationDraftStore.getSelfParticipantId(CONVERSATION_ID)
            } returns "self-1"
            every {
                conversationDraftStore.readDraftMessage(
                    conversationId = CONVERSATION_ID,
                    selfParticipantId = "self-1",
                )
            } returns createDraftAudioMessageData()
            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )

            repository.observeConversationDraft(conversationId = CONVERSATION_ID).test {
                assertEquals(
                    3210L,
                    awaitItem().attachments.single().durationMillis,
                )
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                anyConstructed<MediaMetadataRetrieverWrapper>().setDataSource(any())
            }
        }
    }

    @Test
    fun observeConversationDraft_skipsAudioMetadataResolverWhenDraftHasNoAudioAttachments() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)
            val repository = createRepository()

            mockkConstructor(MediaMetadataRetrieverWrapper::class)

            every {
                conversationDraftStore.getSelfParticipantId(CONVERSATION_ID)
            } returns "self-1"
            every {
                conversationDraftStore.readDraftMessage(
                    conversationId = CONVERSATION_ID,
                    selfParticipantId = "self-1",
                )
            } returns createDraftImageMessageData()
            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )

            repository.observeConversationDraft(conversationId = CONVERSATION_ID).test {
                assertEquals("image/jpeg", awaitItem().attachments.single().contentType)
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 0) {
                anyConstructed<MediaMetadataRetrieverWrapper>().setDataSource(any())
            }
        }
    }

    @Test
    fun saveDraft_bindsMissingParticipantsAndNotifiesMetadata() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val updatedMessage = slot<MessageData>()
            val repository = createRepository()

            every {
                conversationDraftStore.getSelfParticipantId(CONVERSATION_ID)
            } returns "self-1"
            every {
                conversationDraftStore.updateDraftMessage(
                    conversationId = CONVERSATION_ID,
                    message = capture(updatedMessage),
                )
            } just runs

            repository.saveDraft(
                conversationId = CONVERSATION_ID,
                draft = ConversationDraft(
                    messageText = "Hello",
                    selfParticipantId = "",
                ),
            )

            assertEquals("self-1", updatedMessage.captured.selfId)
            assertEquals("self-1", updatedMessage.captured.participantId)
            verify(exactly = 1) {
                MessagingContentProvider.notifyConversationMetadataChanged(CONVERSATION_ID)
            }
        }
    }

    @Test
    fun saveDraft_returnsWithoutPersistingWhenConversationWasDeletedBeforeBinding() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repository = createRepository()

            every {
                conversationDraftStore.getSelfParticipantId(CONVERSATION_ID)
            } returns null

            repository.saveDraft(
                conversationId = CONVERSATION_ID,
                draft = ConversationDraft(
                    messageText = "Hello",
                    selfParticipantId = "",
                ),
            )

            verify(exactly = 0) {
                conversationDraftStore.updateDraftMessage(any(), any())
            }
            verify(exactly = 0) {
                MessagingContentProvider.notifyConversationMetadataChanged(any())
            }
        }
    }

    @Test
    fun saveDraft_preservesProvidedSelfParticipantIdWithoutStoreLookup() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val updatedMessage = slot<MessageData>()
            val repository = createRepository()

            every {
                conversationDraftStore.updateDraftMessage(
                    conversationId = CONVERSATION_ID,
                    message = capture(updatedMessage),
                )
            } just runs

            repository.saveDraft(
                conversationId = CONVERSATION_ID,
                draft = ConversationDraft(
                    messageText = "Hello",
                    selfParticipantId = "self-2",
                ),
            )

            assertEquals("self-2", updatedMessage.captured.selfId)
            assertEquals("self-2", updatedMessage.captured.participantId)
            verify(exactly = 0) {
                conversationDraftStore.getSelfParticipantId(CONVERSATION_ID)
            }
        }
    }

    private fun createRepository(): ConversationDraftsRepositoryImpl {
        return ConversationDraftsRepositoryImpl(
            contentResolver = contentResolver,
            conversationDraftMessageDataMapper = ConversationDraftMessageDataMapperImpl(),
            conversationMessageDataDraftMapper = ConversationMessageDataDraftMapperImpl(),
            conversationDraftStore = conversationDraftStore,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            messagingDbDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun stubObserverRegistration(
        expectedUri: Uri,
        registeredObserver: CapturingSlot<ContentObserver>,
    ) {
        every {
            contentResolver.registerContentObserver(
                expectedUri,
                true,
                capture(registeredObserver),
            )
        } just runs
        every {
            contentResolver.unregisterContentObserver(any())
        } just runs
    }

    private fun createDraftAudioMessageData(): MessageData {
        return MessageData.createDraftMmsMessage(
            CONVERSATION_ID,
            "self-1",
            "",
            "",
        ).apply {
            addPart(
                MessagePartData.createMediaMessagePart(
                    "audio/3gpp",
                    Uri.parse("content://media/audio/1"),
                    0,
                    0,
                ),
            )
        }
    }

    private fun createDraftImageMessageData(): MessageData {
        return MessageData.createDraftMmsMessage(
            CONVERSATION_ID,
            "self-1",
            "",
            "",
        ).apply {
            addPart(
                MessagePartData.createMediaMessagePart(
                    "image/jpeg",
                    Uri.parse("content://media/image/1"),
                    640,
                    480,
                ),
            )
        }
    }
}
