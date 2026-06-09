package com.android.messaging.ui.conversation.composer.delegate.draft

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.domain.conversation.usecase.action.CheckConversationActionRequirements
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.domain.conversation.usecase.draft.ResolveConversationDraftSendProtocol
import com.android.messaging.domain.conversation.usecase.draft.ResolveDraftAttachmentsWithinLimit
import com.android.messaging.domain.conversation.usecase.draft.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftDelegateImpl
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftEditorDelegateImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationDraftDelegateSimSelectionTest {

    private val checkConversationActionRequirements = mockk<CheckConversationActionRequirements>()
    private val conversationDraftsRepository = mockk<ConversationDraftsRepository>()
    private val sendConversationDraft = mockk<SendConversationDraft>()
    private val subscriptionsRepository = mockk<SubscriptionsRepository>()
    private val resolveConversationDraftSendProtocol = mockk<ResolveConversationDraftSendProtocol>()
    private val resolveDraftAttachmentsWithinLimit = mockk<ResolveDraftAttachmentsWithinLimit>()

    private val sentDraft = slot<ConversationDraft>()

    @Before
    fun setUp() {
        every {
            checkConversationActionRequirements()
        } returns ConversationActionRequirementsResult.Ready

        every {
            conversationDraftsRepository.observeConversationDraft(conversationId = any())
        } returns flowOf(ConversationDraft())

        coEvery {
            resolveConversationDraftSendProtocol(conversationId = any(), draft = any())
        } returns ConversationDraftSendProtocol.SMS

        every {
            sendConversationDraft(
                conversationId = any(),
                draft = capture(sentDraft),
                ignoreMessageSizeLimit = any(),
            )
        } returns flowOf(Unit)
    }

    @Test
    fun simPickedBeforeDraftEditorBindsToConversationIsUsedWhenSendingDraft() = runTest {
        val delegate = createDelegate()
        val conversationIdFlow = MutableStateFlow<String?>(CONVERSATION_ID)

        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)
        delegate.onSelfParticipantIdChanged(
            conversationId = CONVERSATION_ID,
            selfParticipantId = PICKED_SELF_PARTICIPANT_ID,
        )
        runCurrent()

        delegate.onMessageTextChanged(messageText = "hello")
        delegate.onSendClick()
        runCurrent()

        assertEquals(PICKED_SELF_PARTICIPANT_ID, sentDraft.captured.selfParticipantId)
    }

    @Test
    fun simPickedAfterDraftEditorBindsToConversationIsUsedWhenSendingDraft() = runTest {
        val delegate = createDelegate()
        val conversationIdFlow = MutableStateFlow<String?>(CONVERSATION_ID)

        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)
        runCurrent()
        delegate.onSelfParticipantIdChanged(
            conversationId = CONVERSATION_ID,
            selfParticipantId = PICKED_SELF_PARTICIPANT_ID,
        )

        delegate.onMessageTextChanged(messageText = "hello")
        delegate.onSendClick()
        runCurrent()

        assertEquals(PICKED_SELF_PARTICIPANT_ID, sentDraft.captured.selfParticipantId)
    }

    @Test
    fun simPickedBeforeDraftEditorBindsOverridesSeededDraftSelfParticipantId() = runTest {
        val delegate = createDelegate()
        val conversationIdFlow = MutableStateFlow<String?>(CONVERSATION_ID)

        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)
        delegate.seedDraft(
            conversationId = CONVERSATION_ID,
            draft = ConversationDraft(
                messageText = "hello",
                selfParticipantId = SEEDED_SELF_PARTICIPANT_ID,
            ),
        )
        delegate.onSelfParticipantIdChanged(
            conversationId = CONVERSATION_ID,
            selfParticipantId = PICKED_SELF_PARTICIPANT_ID,
        )
        runCurrent()

        delegate.onSendClick()
        runCurrent()

        assertEquals("hello", sentDraft.captured.messageText)
        assertEquals(PICKED_SELF_PARTICIPANT_ID, sentDraft.captured.selfParticipantId)
    }

    @Test
    fun simPickedForAnotherConversationIsNotAppliedToBoundConversation() = runTest {
        val delegate = createDelegate()
        val conversationIdFlow = MutableStateFlow<String?>(CONVERSATION_ID)

        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)
        delegate.onSelfParticipantIdChanged(
            conversationId = "another-conversation",
            selfParticipantId = PICKED_SELF_PARTICIPANT_ID,
        )
        runCurrent()

        delegate.onMessageTextChanged(messageText = "hello")
        delegate.onSendClick()
        runCurrent()

        assertEquals("", sentDraft.captured.selfParticipantId)
    }

    private fun TestScope.createDelegate(): ConversationDraftDelegateImpl {
        val conversationDraftEditorDelegate = ConversationDraftEditorDelegateImpl(
            subscriptionsRepository = subscriptionsRepository,
            resolveConversationDraftSendProtocol = resolveConversationDraftSendProtocol,
            resolveDraftAttachmentsWithinLimit = resolveDraftAttachmentsWithinLimit,
        )

        return ConversationDraftDelegateImpl(
            applicationScope = backgroundScope,
            checkConversationActionRequirements = checkConversationActionRequirements,
            conversationDraftsRepository = conversationDraftsRepository,
            conversationDraftEditorDelegate = conversationDraftEditorDelegate,
            sendConversationDraft = sendConversationDraft,
            defaultDispatcher = StandardTestDispatcher(scheduler = testScheduler),
        )
    }

    private companion object {
        private const val CONVERSATION_ID = "conversation-1"
        private const val PICKED_SELF_PARTICIPANT_ID = "self-participant-2"
        private const val SEEDED_SELF_PARTICIPANT_ID = "self-participant-1"
    }
}
