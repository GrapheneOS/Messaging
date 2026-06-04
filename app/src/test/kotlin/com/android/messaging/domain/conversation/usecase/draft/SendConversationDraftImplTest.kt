package com.android.messaging.domain.conversation.usecase.draft

import android.net.Uri
import app.cash.turbine.test
import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapper
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.metadata.ConversationMetadata
import com.android.messaging.data.conversation.model.send.ConversationSendData
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.datamodel.action.InsertNewMessageAction
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.domain.conversation.usecase.draft.exception.BlankConversationIdException
import com.android.messaging.domain.conversation.usecase.draft.exception.ConversationRecipientsNotLoadedException
import com.android.messaging.domain.conversation.usecase.draft.exception.ConversationSimNotReadyException
import com.android.messaging.domain.conversation.usecase.draft.exception.DraftDispatchFailedException
import com.android.messaging.domain.conversation.usecase.draft.exception.EmptyConversationDraftException
import com.android.messaging.domain.conversation.usecase.draft.exception.MessageLimitExceededException
import com.android.messaging.domain.conversation.usecase.draft.exception.MissingSelfPhoneNumberForGroupMmsException
import com.android.messaging.domain.conversation.usecase.draft.exception.TooManyVideoAttachmentsException
import com.android.messaging.domain.conversation.usecase.draft.exception.UnknownConversationRecipientException
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.sms.MmsUtils
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TEST_SELF_SUB_ID as SELF_SUB_ID
import com.android.messaging.testutil.createConversationMetadata
import com.android.messaging.util.PhoneUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SendConversationDraftImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val defaultPhoneUtils = mockk<PhoneUtils>(relaxed = true)

    @Before
    fun setUp() {
        unmockkAll()
        mockkStatic(InsertNewMessageAction::class)
        mockkStatic(PhoneUtils::class)
        every { InsertNewMessageAction.insertNewMessage(any()) } just runs
        every {
            InsertNewMessageAction.insertNewMessage(
                any<MessageData>(),
                any(),
            )
        } just runs
        every { PhoneUtils.getDefault() } returns defaultPhoneUtils
        every {
            defaultPhoneUtils.defaultSmsSubscriptionId
        } returns ParticipantData.DEFAULT_SELF_SUB_ID
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_throwsWhenConversationIdIsBlank() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val exception = collectFailure(
                createUseCase().invoke(
                    conversationId = " ",
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ),
            )

            assertEquals(BlankConversationIdException::class.java, exception.javaClass)
        }
    }

    @Test
    fun invoke_throwsWhenDraftIsEmpty() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val exception = collectFailure(
                createUseCase().invoke(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(),
                ),
            )

            assertEquals(EmptyConversationDraftException::class.java, exception.javaClass)
        }
    }

    @Test
    fun invoke_isColdUntilCollected() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repository = createConversationsRepositoryMock()
            val mapper = createConversationDraftMessageDataMapperMock()
            val getSendProtocol = createGetSendProtocolMock()
            val useCase = createUseCase(
                repository = repository,
                mapper = mapper,
                getSendProtocol = getSendProtocol,
            )

            @Suppress("UnusedFlow")
            useCase.invoke(
                conversationId = CONVERSATION_ID,
                draft = ConversationDraft(
                    messageText = "Hello",
                ),
            )

            coVerify(exactly = 0) {
                repository.getConversationSendData(any(), any())
            }
            verify(exactly = 0) {
                getSendProtocol.invoke(any(), any())
            }
            verify(exactly = 0) {
                mapper.map(
                    conversationId = any(),
                    draft = any(),
                    forceMms = any(),
                )
            }
            verify(exactly = 0) {
                InsertNewMessageAction.insertNewMessage(any())
            }
        }
    }

    @Test
    fun invoke_mapsDraftAndSendsMessageOnCollection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messageData = createMessageData()
            val sendData = createSendData()
            val repository = createConversationsRepositoryMock(sendData = sendData)
            val mapper = createConversationDraftMessageDataMapperMock(
                messageToReturn = messageData,
            )
            val getSendProtocol = createGetSendProtocolMock(
                protocol = ConversationDraftSendProtocol.SMS,
            )
            val draft = ConversationDraft(
                messageText = "Hello",
                selfParticipantId = "self-1",
            )
            val useCase = createUseCase(
                repository = repository,
                mapper = mapper,
                getSendProtocol = getSendProtocol,
                dispatcher = mainDispatcherRule.testDispatcher,
            )

            useCase.invoke(
                conversationId = CONVERSATION_ID,
                draft = draft,
            ).test {
                assertEquals(Unit, awaitItem())
                awaitComplete()
            }

            coVerify(exactly = 1) {
                repository.getConversationSendData(
                    conversationId = CONVERSATION_ID,
                    requestedSelfParticipantId = "self-1",
                )
            }
            verify(exactly = 1) {
                getSendProtocol.invoke(
                    draft = draft,
                    sendData = sendData,
                )
            }
            verify(exactly = 1) {
                mapper.map(
                    conversationId = CONVERSATION_ID,
                    draft = draft,
                    forceMms = false,
                )
            }
            verify(exactly = 1) {
                InsertNewMessageAction.insertNewMessage(messageData)
            }
        }
    }

    @Test
    fun invoke_forcesMmsWhenProtocolUseCaseReturnsMms() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val mapper = createConversationDraftMessageDataMapperMock()
            val draft = ConversationDraft(
                messageText = "Hello",
            )
            val useCase = createUseCase(
                mapper = mapper,
                getSendProtocol = createGetSendProtocolMock(
                    protocol = ConversationDraftSendProtocol.MMS,
                ),
            )

            useCase.invoke(
                conversationId = CONVERSATION_ID,
                draft = draft,
            ).test {
                assertEquals(Unit, awaitItem())
                awaitComplete()
            }

            verify(exactly = 1) {
                mapper.map(
                    conversationId = CONVERSATION_ID,
                    draft = draft,
                    forceMms = true,
                )
            }
        }
    }

    @Test
    fun invoke_throwsWhenConversationSendDataIsMissing() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val exception = collectFailure(
                createUseCase(
                    repository = createConversationsRepositoryMock(sendData = null),
                ).invoke(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ),
            )

            assertEquals(
                ConversationRecipientsNotLoadedException::class.java,
                exception.javaClass,
            )
        }
    }

    @Test
    fun invoke_throwsWhenParticipantsAreNotLoaded() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val exception = collectFailure(
                createUseCase(
                    repository = createConversationsRepositoryMock(
                        sendData = createSendData(
                            participants = createParticipantsData(loaded = false),
                        ),
                    ),
                ).invoke(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ),
            )

            assertEquals(
                ConversationRecipientsNotLoadedException::class.java,
                exception.javaClass,
            )
        }
    }

    @Test
    fun invoke_throwsWhenConversationContainsUnknownRecipient() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val unknownParticipant = mockk<ParticipantData>()
            every { unknownParticipant.isUnknownSender } returns true
            val exception = collectFailure(
                createUseCase(
                    repository = createConversationsRepositoryMock(
                        sendData = createSendData(
                            participants = createParticipantsData(
                                participantList = listOf(unknownParticipant),
                            ),
                        ),
                    ),
                ).invoke(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ),
            )

            assertEquals(UnknownConversationRecipientException::class.java, exception.javaClass)
        }
    }

    @Test
    fun invoke_throwsWhenGroupMmsSelfPhoneNumberIsMissing() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            every { PhoneUtils.get(SELF_SUB_ID) } returns defaultPhoneUtils
            every { defaultPhoneUtils.getSelfRawNumber(true) } returns null

            val exception = collectFailure(
                createUseCase(
                    repository = createConversationsRepositoryMock(
                        sendData = createSendData(
                            metadata = createConversationMetadata(isGroupConversation = true),
                        ),
                    ),
                    getSendProtocol = createGetSendProtocolMock(
                        protocol = ConversationDraftSendProtocol.MMS,
                    ),
                ).invoke(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ),
            )

            assertEquals(
                MissingSelfPhoneNumberForGroupMmsException::class.java,
                exception.javaClass,
            )
        }
    }

    @Test
    fun invoke_throwsWhenGroupMmsSelfPhoneNumberCannotBeRead() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            every { PhoneUtils.get(SELF_SUB_ID) } returns defaultPhoneUtils
            every {
                defaultPhoneUtils.getSelfRawNumber(true)
            } throws IllegalStateException("sim not ready")

            val exception = collectFailure(
                createUseCase(
                    repository = createConversationsRepositoryMock(
                        sendData = createSendData(
                            metadata = createConversationMetadata(isGroupConversation = true),
                        ),
                    ),
                    getSendProtocol = createGetSendProtocolMock(
                        protocol = ConversationDraftSendProtocol.MMS,
                    ),
                ).invoke(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ),
            )

            assertEquals(ConversationSimNotReadyException::class.java, exception.javaClass)
            assertEquals("sim not ready", exception.cause?.message)
        }
    }

    @Test
    fun invoke_throwsWhenDraftHasTooManyVideoAttachments() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val videoAttachments = List(MmsUtils.MAX_VIDEO_ATTACHMENT_COUNT + 1) { index ->
                ConversationDraftAttachment(
                    contentType = "video/mp4",
                    contentUri = "content://video/$index",
                )
            }.toPersistentList()

            val exception = collectFailure(
                createUseCase().invoke(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(
                        attachments = videoAttachments,
                    ),
                ),
            )

            assertEquals(TooManyVideoAttachmentsException::class.java, exception.javaClass)
        }
    }

    @Test
    fun invoke_throwsWhenMappedMessageExceedsAttachmentLimit() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messageData = createMessageDataWithAttachments(attachmentCount = 2)
            val subscriptionsRepository = createSubscriptionsRepositoryMock(attachmentLimit = 1)
            val useCase = createUseCase(
                subscriptionsRepository = subscriptionsRepository,
                mapper = createConversationDraftMessageDataMapperMock(
                    messageToReturn = messageData,
                ),
            )

            val exception = collectFailure(
                useCase.invoke(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ),
            )

            assertEquals(MessageLimitExceededException::class.java, exception.javaClass)
            verify(exactly = 0) {
                InsertNewMessageAction.insertNewMessage(any<MessageData>())
            }
            verify(exactly = 0) {
                InsertNewMessageAction.insertNewMessage(
                    any<MessageData>(),
                    any(),
                )
            }
        }
    }

    @Test
    fun invoke_locksDefaultSelfMessageToSystemDefaultSubscription() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messageData = createMessageData()
            every { defaultPhoneUtils.defaultSmsSubscriptionId } returns SELF_SUB_ID
            val useCase = createUseCase(
                mapper = createConversationDraftMessageDataMapperMock(
                    messageToReturn = messageData,
                ),
                repository = createConversationsRepositoryMock(
                    sendData = createSendData(
                        selfParticipant = ParticipantData.getSelfParticipant(
                            ParticipantData.DEFAULT_SELF_SUB_ID,
                        ),
                    ),
                ),
            )

            useCase.invoke(
                conversationId = CONVERSATION_ID,
                draft = ConversationDraft(
                    messageText = "Hello",
                ),
            ).test {
                assertEquals(Unit, awaitItem())
                awaitComplete()
            }

            verify(exactly = 1) {
                InsertNewMessageAction.insertNewMessage(messageData, SELF_SUB_ID)
            }
            verify(exactly = 0) {
                InsertNewMessageAction.insertNewMessage(messageData)
            }
        }
    }

    @Test
    fun invoke_wrapsMapperFailures() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val mapper = createConversationDraftMessageDataMapperMock(
                failure = IllegalStateException("mapper failure"),
            )
            val useCase = createUseCase(mapper = mapper)

            val exception = collectFailure(
                useCase.invoke(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ),
            )

            assertEquals(DraftDispatchFailedException::class.java, exception.javaClass)
            assertEquals("mapper failure", exception.cause?.message)
        }
    }

    @Test
    fun invoke_wrapsSenderFailures() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            every {
                InsertNewMessageAction.insertNewMessage(any())
            } throws IllegalStateException("sender failure")
            val useCase = createUseCase()

            val exception = collectFailure(
                useCase.invoke(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ),
            )

            assertEquals(DraftDispatchFailedException::class.java, exception.javaClass)
            assertEquals("sender failure", exception.cause?.message)
        }
    }

    @Test
    fun invoke_rethrowsCancellation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val cancellationException = CancellationException("cancelled")
            every {
                InsertNewMessageAction.insertNewMessage(any())
            } throws cancellationException
            val useCase = createUseCase()

            val exception = collectFailure(
                useCase.invoke(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ),
            )

            assertEquals(CancellationException::class.java, exception.javaClass)
            assertEquals(cancellationException.message, exception.message)
        }
    }

    private suspend fun collectFailure(flow: Flow<Unit>): Throwable {
        try {
            flow.collect()
        } catch (exception: Throwable) {
            return exception
        }

        fail("Expected flow collection to fail.")
        error("Unreachable")
    }

    private fun createUseCase(
        repository: ConversationsRepository = createConversationsRepositoryMock(),
        subscriptionsRepository: SubscriptionsRepository = createSubscriptionsRepositoryMock(),
        mapper: ConversationDraftMessageDataMapper = createConversationDraftMessageDataMapperMock(),
        getSendProtocol: GetConversationDraftSendProtocol = createGetSendProtocolMock(),
        dispatcher: TestDispatcher = mainDispatcherRule.testDispatcher,
    ): SendConversationDraftImpl {
        return SendConversationDraftImpl(
            conversationsRepository = repository,
            subscriptionsRepository = subscriptionsRepository,
            getConversationDraftSendProtocol = getSendProtocol,
            conversationDraftMessageDataMapper = mapper,
            ioDispatcher = dispatcher,
        )
    }

    private fun createConversationsRepositoryMock(
        sendData: ConversationSendData? = createSendData(),
    ): ConversationsRepository {
        val repository = mockk<ConversationsRepository>(relaxed = true)
        coEvery {
            repository.getConversationSendData(
                conversationId = any(),
                requestedSelfParticipantId = any(),
            )
        } returns sendData
        return repository
    }

    private fun createSubscriptionsRepositoryMock(
        attachmentLimit: Int = Int.MAX_VALUE,
    ): SubscriptionsRepository {
        val repository = mockk<SubscriptionsRepository>(relaxed = true)
        every { repository.resolveAttachmentLimit() } returns attachmentLimit
        return repository
    }

    private fun createGetSendProtocolMock(
        protocol: ConversationDraftSendProtocol = ConversationDraftSendProtocol.SMS,
    ): GetConversationDraftSendProtocol {
        val getSendProtocol = mockk<GetConversationDraftSendProtocol>()
        every {
            getSendProtocol.invoke(
                draft = any(),
                sendData = any(),
            )
        } returns protocol
        return getSendProtocol
    }

    private fun createConversationDraftMessageDataMapperMock(
        messageToReturn: MessageData = createMessageData(),
        failure: Exception? = null,
    ): ConversationDraftMessageDataMapper {
        val mapper = mockk<ConversationDraftMessageDataMapper>()
        every {
            mapper.map(
                conversationId = any(),
                draft = any(),
                forceMms = any(),
            )
        } answers {
            failure?.let { exception ->
                throw exception
            }
            messageToReturn
        }
        return mapper
    }

    private fun createSendData(
        metadata: ConversationMetadata = createConversationMetadata(),
        participants: ConversationParticipantsData = createParticipantsData(),
        selfParticipant: ParticipantData? = ParticipantData.getSelfParticipant(SELF_SUB_ID),
    ): ConversationSendData {
        return ConversationSendData(
            metadata = metadata,
            participants = participants,
            selfParticipant = selfParticipant,
        )
    }

    private fun createParticipantsData(
        loaded: Boolean = true,
        participantList: List<ParticipantData> = emptyList(),
    ): ConversationParticipantsData {
        val participants = mockk<ConversationParticipantsData>()
        every { participants.isLoaded } returns loaded
        every { participants.iterator() } returns participantList.toMutableList().iterator()
        return participants
    }

    private fun createMessageData(): MessageData {
        return MessageData.createDraftSmsMessage(
            CONVERSATION_ID,
            "self-1",
            "Hello",
        )
    }

    private fun createMessageDataWithAttachments(attachmentCount: Int): MessageData {
        return MessageData.createDraftMmsMessage(
            CONVERSATION_ID,
            "self-1",
            "Hello",
            "",
        ).apply {
            repeat(attachmentCount) { index ->
                addPart(
                    MessagePartData.createMediaMessagePart(
                        "image/jpeg",
                        Uri.parse("content://media/image/$index"),
                        640,
                        480,
                    ),
                )
            }
        }
    }
}
