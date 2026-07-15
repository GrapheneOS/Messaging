package com.android.messaging.ui.conversation.composer.delegate.draft

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.send.ConversationSendData
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.domain.conversation.usecase.action.CheckConversationActionRequirements
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.domain.conversation.usecase.draft.GetConversationDraftSendProtocol
import com.android.messaging.domain.conversation.usecase.draft.ResolveConversationDraftSendProtocolImpl
import com.android.messaging.domain.conversation.usecase.draft.ResolveDraftAttachmentsWithinLimitImpl
import com.android.messaging.domain.conversation.usecase.draft.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftDelegateImpl
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftEditorDelegateImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseConversationDraftDelegateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected suspend fun TestScope.createBoundLoadedDelegateHarness(
        sendConversationDraft: SendConversationDraft = createSendConversationDraftMock(),
        actionRequirements: CheckConversationActionRequirements = createActionRequirementsMock(),
        conversationsRepository: ConversationsRepository = createConversationsRepositoryMock(),
        getDraftSendProtocol: GetConversationDraftSendProtocol = createGetDraftSendProtocolMock(),
    ): DelegateHarness {
        val harness = createHarness(
            sendConversationDraft = sendConversationDraft,
            actionRequirements = actionRequirements,
            conversationsRepository = conversationsRepository,
            getDraftSendProtocol = getDraftSendProtocol,
        )
        harness.conversationIdFlow.value = CONVERSATION_ID
        harness.emitDraft(
            conversationId = CONVERSATION_ID,
            draft = ConversationDraft(),
        )
        advanceUntilIdle()

        return harness
    }

    protected fun TestScope.createHarness(
        sendConversationDraft: SendConversationDraft = createSendConversationDraftMock(),
        actionRequirements: CheckConversationActionRequirements = createActionRequirementsMock(),
        conversationsRepository: ConversationsRepository = createConversationsRepositoryMock(),
        getDraftSendProtocol: GetConversationDraftSendProtocol = createGetDraftSendProtocolMock(),
        observeFailure: Exception? = null,
    ): DelegateHarness {
        val dispatcher = mainDispatcherRule.testDispatcher
        val applicationScope = TestScope(dispatcher)
        val delegateScope = TestScope(dispatcher)
        val draftFlows = mutableMapOf<String, MutableSharedFlow<ConversationDraft>>()
        val conversationDraftsRepository = createConversationDraftsRepositoryMock(
            draftFlows = draftFlows,
            observeFailure = observeFailure,
        )
        val subscriptionsRepository = createSubscriptionsRepositoryMock()
        val conversationDraftEditorDelegate = ConversationDraftEditorDelegateImpl(
            subscriptionsRepository = subscriptionsRepository,
            resolveConversationDraftSendProtocol = ResolveConversationDraftSendProtocolImpl(
                conversationsRepository = conversationsRepository,
                getConversationDraftSendProtocol = getDraftSendProtocol,
            ),
            resolveDraftAttachmentsWithinLimit = ResolveDraftAttachmentsWithinLimitImpl(
                subscriptionsRepository = subscriptionsRepository,
            ),
        )
        val delegate = ConversationDraftDelegateImpl(
            applicationScope = applicationScope,
            checkConversationActionRequirements = actionRequirements,
            conversationDraftsRepository = conversationDraftsRepository,
            conversationDraftEditorDelegate = conversationDraftEditorDelegate,
            sendConversationDraft = sendConversationDraft,
            defaultDispatcher = dispatcher,
        )
        val conversationIdFlow = MutableStateFlow<String?>(null)

        delegate.bind(
            scope = delegateScope,
            conversationIdFlow = conversationIdFlow,
        )

        return DelegateHarness(
            delegate = delegate,
            conversationDraftsRepository = conversationDraftsRepository,
            draftFlows = draftFlows,
            conversationIdFlow = conversationIdFlow,
            delegateScope = delegateScope,
            applicationScope = applicationScope,
        )
    }

    protected fun createActionRequirementsMock(
        initialResult: ConversationActionRequirementsResult =
            ConversationActionRequirementsResult.Ready,
    ): CheckConversationActionRequirements {
        return createActionRequirementsMock(results = listOf(initialResult))
    }

    protected fun createActionRequirementsMock(
        results: List<ConversationActionRequirementsResult>,
    ): CheckConversationActionRequirements {
        val mock = mockk<CheckConversationActionRequirements>()
        every { mock.invoke() } returnsMany results
        return mock
    }

    protected fun createConversationDraftsRepositoryMock(
        draftFlows: MutableMap<String, MutableSharedFlow<ConversationDraft>>,
        observeFailure: Exception? = null,
    ): ConversationDraftsRepository {
        val repository = mockk<ConversationDraftsRepository>()
        every { repository.observeConversationDraft(conversationId = any()) } answers {
            observeFailure?.let { exception ->
                return@answers flow {
                    throw exception
                }
            }
            draftFlows.getOrPut(firstArg()) {
                MutableSharedFlow(
                    replay = 1,
                    extraBufferCapacity = 16,
                )
            }
        }
        coEvery {
            repository.saveDraft(
                conversationId = any(),
                draft = any(),
            )
        } just runs
        return repository
    }

    protected fun createConversationsRepositoryMock(
        sendData: ConversationSendData? = mockk(relaxed = true),
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

    protected fun createSubscriptionsRepositoryMock(): SubscriptionsRepository {
        val repository = mockk<SubscriptionsRepository>(relaxed = true)
        every { repository.resolveAttachmentLimit() } returns Int.MAX_VALUE
        return repository
    }

    protected fun createGetDraftSendProtocolMock(
        protocol: ConversationDraftSendProtocol = ConversationDraftSendProtocol.SMS,
    ): GetConversationDraftSendProtocol {
        val mock = mockk<GetConversationDraftSendProtocol>()
        every {
            mock.invoke(
                draft = any(),
                sendData = any(),
            )
        } returns protocol
        return mock
    }

    protected fun createSendConversationDraftMock(
        sendResult: Flow<Unit> = createSuccessfulSendFlow(),
    ): SendConversationDraft {
        val mock = mockk<SendConversationDraft>()
        every {
            mock.invoke(
                conversationId = any(),
                draft = any(),
                ignoreMessageSizeLimit = any(),
            )
        } returns sendResult
        return mock
    }

    protected fun createSuccessfulSendFlow(): Flow<Unit> {
        return flow {
            emit(Unit)
        }
    }

    protected data class DelegateHarness(
        val delegate: ConversationDraftDelegateImpl,
        val conversationDraftsRepository: ConversationDraftsRepository,
        val draftFlows: MutableMap<String, MutableSharedFlow<ConversationDraft>>,
        val conversationIdFlow: MutableStateFlow<String?>,
        val delegateScope: TestScope,
        val applicationScope: TestScope,
    ) {
        suspend fun emitDraft(
            conversationId: String,
            draft: ConversationDraft,
        ) {
            draftFlows.getOrPut(conversationId) {
                MutableSharedFlow(
                    replay = 1,
                    extraBufferCapacity = 16,
                )
            }.emit(draft)
        }

        fun cancel() {
            delegateScope.cancel()
            applicationScope.cancel()
        }
    }
}
