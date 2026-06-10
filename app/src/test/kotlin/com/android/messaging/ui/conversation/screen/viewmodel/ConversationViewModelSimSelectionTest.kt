package com.android.messaging.ui.conversation.screen.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription
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
import com.android.messaging.ui.conversation.composer.delegate.ConversationSubscriptionSelectionDelegateImpl
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
import com.android.messaging.ui.conversation.screen.ConversationViewModel
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
            conversationComposerUiStateMapper.map(
                audioRecording = any(),
                draftState = any(),
                attachments = any(),
                composerAvailability = any(),
                subscriptions = any(),
                areSubscriptionsLoaded = any(),
                defaultSmsSubscriptionId = any(),
            )
        } returns ConversationComposerUiState()

        every { subscriptionsRepository.observeActiveSubscriptions() } returns emptyFlow()
        every { subscriptionsRepository.observeDefaultSmsSubscriptionId() } returns emptyFlow()
        every { subscriptionsRepository.getDefaultSmsSubscriptionId() } returns DEFAULT_SUB_ID
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

    @Test
    fun composerState_withSystemDefaultSecondSim_resolvesDefaultSmsSubscription() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        every { subscriptionsRepository.observeActiveSubscriptions() } returns flowOf(
            persistentListOf(
                firstSubscription(),
                secondSubscription(),
            ),
        )
        every { subscriptionsRepository.observeDefaultSmsSubscriptionId() } returns flowOf(
            SECOND_SUB_ID,
        )

        val viewModel = createViewModel()
        val collectionJob = launch {
            viewModel.scaffoldUiState.collect {}
        }

        runCurrent()

        verify(atLeast = 1) {
            conversationComposerUiStateMapper.map(
                audioRecording = any(),
                draftState = any(),
                attachments = any(),
                composerAvailability = any(),
                subscriptions = any(),
                areSubscriptionsLoaded = true,
                defaultSmsSubscriptionId = SECOND_SUB_ID,
            )
        }

        collectionJob.cancel()
    }

    @Test
    fun composerState_afterDefaultSmsSubscriptionChange_remapsWithNewDefault() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val defaultSmsSubscriptionIdFlow = MutableStateFlow(FIRST_SUB_ID)
        every { subscriptionsRepository.observeActiveSubscriptions() } returns flowOf(
            persistentListOf(
                firstSubscription(),
                secondSubscription(),
            ),
        )
        every {
            subscriptionsRepository.observeDefaultSmsSubscriptionId()
        } returns defaultSmsSubscriptionIdFlow

        val viewModel = createViewModel()
        val collectionJob = launch {
            viewModel.scaffoldUiState.collect {}
        }

        runCurrent()

        verify(atLeast = 1) {
            conversationComposerUiStateMapper.map(
                audioRecording = any(),
                draftState = any(),
                attachments = any(),
                composerAvailability = any(),
                subscriptions = any(),
                areSubscriptionsLoaded = true,
                defaultSmsSubscriptionId = FIRST_SUB_ID,
            )
        }

        defaultSmsSubscriptionIdFlow.value = SECOND_SUB_ID
        runCurrent()

        verify(atLeast = 1) {
            conversationComposerUiStateMapper.map(
                audioRecording = any(),
                draftState = any(),
                attachments = any(),
                composerAvailability = any(),
                subscriptions = any(),
                areSubscriptionsLoaded = true,
                defaultSmsSubscriptionId = SECOND_SUB_ID,
            )
        }

        collectionJob.cancel()
    }

    private fun createViewModel(): ConversationViewModel {
        val subscriptionSelectionDelegate = ConversationSubscriptionSelectionDelegateImpl(
            subscriptionsRepository = subscriptionsRepository,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
        return ConversationViewModel(
            conversationAudioRecordingDelegate = conversationAudioRecordingDelegate,
            conversationComposerAttachmentsDelegate = conversationComposerAttachmentsDelegate,
            conversationDraftDelegate = conversationDraftDelegate,
            conversationMessagesDelegate = conversationMessagesDelegate,
            conversationMessageSelectionDelegate = conversationMessageSelectionDelegate,
            conversationMediaPickerDelegate = conversationMediaPickerDelegate,
            conversationMetadataDelegate = conversationMetadataDelegate,
            conversationFocusDelegate = conversationFocusDelegate,
            conversationSubscriptionSelectionDelegate = subscriptionSelectionDelegate,
            conversationComposerUiStateMapper = conversationComposerUiStateMapper,
            simSelectionRepository = simSelectionRepository,
            canAddMoreConversationParticipants = canAddMoreConversationParticipants,
            createDefaultSmsRoleRequest = createDefaultSmsRoleRequest,
            isDeviceVoiceCapable = isDeviceVoiceCapable,
            isEmergencyPhoneNumber = isEmergencyPhoneNumber,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            savedStateHandle = SavedStateHandle(),
        )
    }

    private fun firstSubscription(): Subscription {
        return Subscription(
            selfParticipantId = FIRST_SELF_PARTICIPANT_ID,
            subId = FIRST_SUB_ID,
            label = ConversationSubscriptionLabel.Named(name = "SIM 1"),
            displayDestination = null,
            displaySlotId = 1,
            color = 0,
        )
    }

    private fun secondSubscription(): Subscription {
        return Subscription(
            selfParticipantId = SECOND_SELF_PARTICIPANT_ID,
            subId = SECOND_SUB_ID,
            label = ConversationSubscriptionLabel.Named(name = "SIM 2"),
            displayDestination = null,
            displaySlotId = 2,
            color = 0,
        )
    }

    private companion object {
        private const val CONVERSATION_ID = "conversation-1"
        private const val PICKED_SELF_PARTICIPANT_ID = "self-participant-2"
        private const val FIRST_SELF_PARTICIPANT_ID = "self-participant-1"
        private const val SECOND_SELF_PARTICIPANT_ID = "self-participant-2"
        private const val DEFAULT_SUB_ID = -1
        private const val FIRST_SUB_ID = 1
        private const val SECOND_SUB_ID = 2
    }
}
