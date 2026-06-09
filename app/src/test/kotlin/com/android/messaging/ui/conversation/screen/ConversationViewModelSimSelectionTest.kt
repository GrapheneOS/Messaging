package com.android.messaging.ui.conversation.screen

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.subscription.repository.ConversationSimSelectionRepository
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.domain.conversation.usecase.action.CreateDefaultSmsRoleRequest
import com.android.messaging.domain.conversation.usecase.participant.CanAddMoreConversationParticipants
import com.android.messaging.domain.conversation.usecase.telephony.IsDeviceVoiceCapable
import com.android.messaging.domain.conversation.usecase.telephony.IsEmergencyPhoneNumber
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.audio.delegate.ConversationAudioRecordingDelegate
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.composer.delegate.ConversationComposerAttachmentsDelegate
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.focus.delegate.ConversationFocusDelegate
import com.android.messaging.ui.conversation.mediapicker.delegate.ConversationMediaPickerDelegate
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessageSelectionDelegate
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

internal class ConversationViewModelSimSelectionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val conversationAudioRecordingDelegate = mockk<ConversationAudioRecordingDelegate>()
    private val conversationComposerAttachmentsDelegate =
        mockk<ConversationComposerAttachmentsDelegate>()
    private val conversationDraftDelegate = mockk<ConversationDraftDelegate>()
    private val conversationMessagesDelegate = mockk<ConversationMessagesDelegate>()
    private val conversationMessageSelectionDelegate =
        mockk<ConversationMessageSelectionDelegate>()
    private val conversationMediaPickerDelegate = mockk<ConversationMediaPickerDelegate>()
    private val conversationMetadataDelegate = mockk<ConversationMetadataDelegate>()
    private val conversationFocusDelegate = mockk<ConversationFocusDelegate>()
    private val conversationComposerUiStateMapper = mockk<ConversationComposerUiStateMapper>()
    private val simSelectionRepository = mockk<ConversationSimSelectionRepository>()
    private val subscriptionsRepository = mockk<SubscriptionsRepository>()
    private val canAddMoreConversationParticipants = mockk<CanAddMoreConversationParticipants>()
    private val createDefaultSmsRoleRequest = mockk<CreateDefaultSmsRoleRequest>()
    private val isDeviceVoiceCapable = mockk<IsDeviceVoiceCapable>()
    private val isEmergencyPhoneNumber = mockk<IsEmergencyPhoneNumber>()

    @Before
    fun setUp() {
        every { conversationAudioRecordingDelegate.state } returns MutableStateFlow(
            ConversationAudioRecordingUiState(),
        )
        every { conversationAudioRecordingDelegate.bind(any(), any()) } just runs

        every { conversationComposerAttachmentsDelegate.state } returns MutableStateFlow(
            persistentListOf(),
        )
        every { conversationComposerAttachmentsDelegate.bind(any(), any()) } just runs

        every { conversationDraftDelegate.state } returns MutableStateFlow(ConversationDraftState())
        every { conversationDraftDelegate.attachmentLimitWarning } returns MutableStateFlow(null)
        every { conversationDraftDelegate.isSubjectDialogVisible } returns MutableStateFlow(false)
        every { conversationDraftDelegate.effects } returns emptyFlow()
        every { conversationDraftDelegate.bind(any(), any()) } just runs
        every {
            conversationDraftDelegate.onSelfParticipantIdChanged(
                conversationId = any(),
                selfParticipantId = any(),
            )
        } just runs

        every { conversationMessagesDelegate.state } returns MutableStateFlow(
            ConversationMessagesUiState.Loading,
        )
        every { conversationMessagesDelegate.bind(any(), any()) } just runs

        every { conversationMessageSelectionDelegate.state } returns MutableStateFlow(
            ConversationMessageSelectionUiState(),
        )
        every { conversationMessageSelectionDelegate.effects } returns emptyFlow()
        every { conversationMessageSelectionDelegate.bind(any(), any()) } just runs
        every { conversationMessageSelectionDelegate.dismissMessageSelection() } just runs

        every {
            conversationMediaPickerDelegate.photoPickerSourceContentUriByAttachmentContentUri
        } returns MutableStateFlow(persistentMapOf())
        every { conversationMediaPickerDelegate.effects } returns emptyFlow()
        every { conversationMediaPickerDelegate.bind(any(), any()) } just runs

        every { conversationMetadataDelegate.state } returns MutableStateFlow(
            ConversationMetadataUiState.Loading,
        )
        every {
            conversationMetadataDelegate.isDeleteConversationConfirmationVisible
        } returns MutableStateFlow(false)
        every { conversationMetadataDelegate.effects } returns emptyFlow()
        every { conversationMetadataDelegate.bind(any(), any()) } just runs

        every { conversationFocusDelegate.bind(any(), any()) } just runs

        every {
            conversationComposerUiStateMapper.map(any(), any(), any(), any(), any(), any())
        } returns ConversationComposerUiState()

        every { subscriptionsRepository.observeActiveSubscriptions() } returns emptyFlow()
        every {
            simSelectionRepository.setSelectedSelfId(
                conversationId = any(),
                selfId = any(),
            )
        } just runs
    }

    @Test
    fun onSimSelected_withConversationId_forwardsSelectionToDraftDelegate() {
        val viewModel = createViewModel()
        viewModel.onConversationIdChanged(conversationId = CONVERSATION_ID)

        viewModel.onSimSelected(selfParticipantId = PICKED_SELF_PARTICIPANT_ID)

        verify(exactly = 1) {
            conversationDraftDelegate.onSelfParticipantIdChanged(
                conversationId = CONVERSATION_ID,
                selfParticipantId = PICKED_SELF_PARTICIPANT_ID,
            )
        }
        verify(exactly = 1) {
            simSelectionRepository.setSelectedSelfId(
                conversationId = CONVERSATION_ID,
                selfId = PICKED_SELF_PARTICIPANT_ID,
            )
        }
    }

    @Test
    fun onSimSelected_withoutConversationId_dropsSelection() {
        val viewModel = createViewModel()

        viewModel.onSimSelected(selfParticipantId = PICKED_SELF_PARTICIPANT_ID)

        verify(exactly = 0) {
            conversationDraftDelegate.onSelfParticipantIdChanged(
                conversationId = any(),
                selfParticipantId = any(),
            )
        }
        verify(exactly = 0) {
            simSelectionRepository.setSelectedSelfId(
                conversationId = any(),
                selfId = any(),
            )
        }
    }

    private fun createViewModel(): ConversationViewModel {
        return ConversationViewModel(
            conversationAudioRecordingDelegate = conversationAudioRecordingDelegate,
            conversationComposerAttachmentsDelegate = conversationComposerAttachmentsDelegate,
            conversationDraftDelegate = conversationDraftDelegate,
            conversationMessagesDelegate = conversationMessagesDelegate,
            conversationMessageSelectionDelegate = conversationMessageSelectionDelegate,
            conversationMediaPickerDelegate = conversationMediaPickerDelegate,
            conversationMetadataDelegate = conversationMetadataDelegate,
            conversationFocusDelegate = conversationFocusDelegate,
            conversationComposerUiStateMapper = conversationComposerUiStateMapper,
            subscriptionsRepository = subscriptionsRepository,
            simSelectionRepository = simSelectionRepository,
            canAddMoreConversationParticipants = canAddMoreConversationParticipants,
            createDefaultSmsRoleRequest = createDefaultSmsRoleRequest,
            isDeviceVoiceCapable = isDeviceVoiceCapable,
            isEmergencyPhoneNumber = isEmergencyPhoneNumber,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            savedStateHandle = SavedStateHandle(),
        )
    }

    private companion object {
        private const val CONVERSATION_ID = "conversation-1"
        private const val PICKED_SELF_PARTICIPANT_ID = "self-participant-2"
    }
}
