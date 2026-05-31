package com.android.messaging.ui.conversation.entry

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapper
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.entry.model.ConversationEntryUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationEntryViewModelTest {

    @Test
    fun launchRequest_setsConversationDraftScrollAndStartupAttachment() {
        val draftData = mockk<MessageData>()
        val mappedDraft = ConversationDraft(messageText = "Hello")
        val mapper = createMapper(
            draftData = draftData,
            mappedDraft = mappedDraft,
        )
        val viewModel = createViewModel(mapper = mapper)

        viewModel.onLaunchRequest(
            launchRequest = ConversationEntryLaunchRequest(
                launchGeneration = 4,
                conversationId = CONVERSATION_ID,
                draftData = draftData,
                startupAttachmentUri = "content://media/1",
                startupAttachmentType = "image/png",
                messagePosition = 12,
            ),
        )

        assertEquals(
            ConversationEntryUiState(
                launchGeneration = 4,
                conversationId = CONVERSATION_ID,
                pendingDraft = mappedDraft,
                pendingScrollPosition = 12,
                pendingStartupAttachment = ConversationEntryStartupAttachment(
                    contentType = "image/png",
                    contentUri = "content://media/1",
                ),
            ),
            viewModel.uiState.value,
        )
        verify(exactly = 1) {
            mapper.map(messageData = draftData)
        }
    }

    @Test
    fun launchRequest_ignoresDuplicateGeneration() {
        val draftData = mockk<MessageData>()
        val mapper = createMapper(
            draftData = draftData,
            mappedDraft = ConversationDraft(messageText = "First"),
        )
        val viewModel = createViewModel(mapper = mapper)

        viewModel.onLaunchRequest(
            launchRequest = ConversationEntryLaunchRequest(
                launchGeneration = 1,
                conversationId = CONVERSATION_ID,
                draftData = draftData,
            ),
        )
        viewModel.onLaunchRequest(
            launchRequest = ConversationEntryLaunchRequest(
                launchGeneration = 1,
                conversationId = "conversation-2",
                draftData = draftData,
            ),
        )

        assertEquals(CONVERSATION_ID, viewModel.uiState.value.conversationId)
        verify(exactly = 1) {
            mapper.map(messageData = draftData)
        }
    }

    @Test
    fun conversationNavigationRequest_setsConversationAndSelfParticipant() {
        val viewModel = createViewModel()

        viewModel.onConversationNavigationRequested(
            conversationId = CONVERSATION_ID,
            pendingSelfParticipantId = "self-1",
        )

        assertEquals(CONVERSATION_ID, viewModel.uiState.value.conversationId)
        assertEquals("self-1", viewModel.uiState.value.pendingSelfParticipantId)
    }

    @Test
    fun consumeCallbacks_clearOnlyMatchingPendingValues() {
        val viewModel = createViewModel()
        viewModel.onLaunchRequest(
            launchRequest = ConversationEntryLaunchRequest(
                launchGeneration = 1,
                conversationId = CONVERSATION_ID,
                draftData = mockk(),
                startupAttachmentUri = "content://media/1",
                startupAttachmentType = "image/png",
                messagePosition = 3,
            ),
        )
        viewModel.onConversationNavigationRequested(
            conversationId = CONVERSATION_ID,
            pendingSelfParticipantId = "self-1",
        )

        viewModel.onDraftPayloadConsumed(conversationId = "other")
        viewModel.onScrollPositionConsumed(conversationId = "other")
        viewModel.onStartupAttachmentConsumed(conversationId = "other")
        viewModel.onPendingSelfParticipantIdConsumed(conversationId = "other")

        assertEquals(
            ConversationDraft(messageText = "Mapped"),
            viewModel.uiState.value.pendingDraft,
        )
        assertEquals(3, viewModel.uiState.value.pendingScrollPosition)
        assertEquals("self-1", viewModel.uiState.value.pendingSelfParticipantId)
        assertEquals(
            ConversationEntryStartupAttachment(
                contentType = "image/png",
                contentUri = "content://media/1",
            ),
            viewModel.uiState.value.pendingStartupAttachment,
        )

        viewModel.onDraftPayloadConsumed(conversationId = CONVERSATION_ID)
        viewModel.onScrollPositionConsumed(conversationId = CONVERSATION_ID)
        viewModel.onStartupAttachmentConsumed(conversationId = CONVERSATION_ID)
        viewModel.onPendingSelfParticipantIdConsumed(conversationId = CONVERSATION_ID)

        assertNull(viewModel.uiState.value.pendingDraft)
        assertNull(viewModel.uiState.value.pendingScrollPosition)
        assertNull(viewModel.uiState.value.pendingSelfParticipantId)
        assertNull(viewModel.uiState.value.pendingStartupAttachment)
    }

    @Test
    fun launchRequestState_survivesViewModelRecreationViaSavedStateHandle() {
        val draftData = mockk<MessageData>()
        val mappedDraft = ConversationDraft(messageText = "Restored")
        val mapper = createMapper(draftData = draftData, mappedDraft = mappedDraft)
        val savedStateHandle = SavedStateHandle()

        createViewModel(mapper = mapper, savedStateHandle = savedStateHandle).onLaunchRequest(
            launchRequest = ConversationEntryLaunchRequest(
                launchGeneration = 4,
                conversationId = CONVERSATION_ID,
                draftData = draftData,
                startupAttachmentUri = "content://media/1",
                startupAttachmentType = "image/png",
                messagePosition = 12,
            ),
        )

        val recreatedViewModel = createViewModel(
            mapper = mapper,
            savedStateHandle = savedStateHandle,
        )

        assertEquals(
            ConversationEntryUiState(
                launchGeneration = 4,
                conversationId = CONVERSATION_ID,
                pendingDraft = mappedDraft,
                pendingScrollPosition = 12,
                pendingStartupAttachment = ConversationEntryStartupAttachment(
                    contentType = "image/png",
                    contentUri = "content://media/1",
                ),
            ),
            recreatedViewModel.uiState.value,
        )
    }

    @Test
    fun conversationNavigationState_survivesViewModelRecreationViaSavedStateHandle() {
        val savedStateHandle = SavedStateHandle()

        createViewModel(savedStateHandle = savedStateHandle).onConversationNavigationRequested(
            conversationId = CONVERSATION_ID,
            pendingSelfParticipantId = "self-1",
        )

        val recreatedViewModel = createViewModel(savedStateHandle = savedStateHandle)

        assertEquals(CONVERSATION_ID, recreatedViewModel.uiState.value.conversationId)
        assertEquals("self-1", recreatedViewModel.uiState.value.pendingSelfParticipantId)
    }

    @Test
    fun duplicateLaunchGeneration_isIgnoredAfterViewModelRecreation() {
        val savedStateHandle = SavedStateHandle()

        createViewModel(savedStateHandle = savedStateHandle).onLaunchRequest(
            launchRequest = ConversationEntryLaunchRequest(
                launchGeneration = 1,
                conversationId = CONVERSATION_ID,
                draftData = mockk(),
            ),
        )

        val recreatedViewModel = createViewModel(savedStateHandle = savedStateHandle)
        recreatedViewModel.onLaunchRequest(
            launchRequest = ConversationEntryLaunchRequest(
                launchGeneration = 1,
                conversationId = "conversation-2",
                draftData = mockk(),
            ),
        )

        assertEquals(CONVERSATION_ID, recreatedViewModel.uiState.value.conversationId)
    }

    private fun createViewModel(
        mapper: ConversationMessageDataDraftMapper = createMapper(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): ConversationEntryViewModel {
        return ConversationEntryViewModel(
            conversationMessageDataDraftMapper = mapper,
            savedStateHandle = savedStateHandle,
        )
    }

    private fun createMapper(
        draftData: MessageData? = null,
        mappedDraft: ConversationDraft = ConversationDraft(messageText = "Mapped"),
    ): ConversationMessageDataDraftMapper {
        val mapper = mockk<ConversationMessageDataDraftMapper>()
        every {
            mapper.map(messageData = any())
        } returns mappedDraft
        if (draftData != null) {
            every {
                mapper.map(messageData = draftData)
            } returns mappedDraft
        }
        return mapper
    }
}
