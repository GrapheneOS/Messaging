package com.android.messaging.ui.conversation.screen.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.subscription.repository.ConversationSimSelectionRepository
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.domain.conversation.usecase.action.CreateDefaultSmsRoleRequest
import com.android.messaging.domain.conversation.usecase.participant.CanAddMoreConversationParticipants
import com.android.messaging.domain.conversation.usecase.telephony.IsDeviceVoiceCapable
import com.android.messaging.domain.conversation.usecase.telephony.IsEmergencyPhoneNumber
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CALL_ACTION_PHONE_NUMBER
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.audio.delegate.ConversationAudioRecordingDelegate
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.composer.delegate.ConversationComposerAttachmentsDelegate
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.composer.delegate.ConversationSubscriptionSelectionDelegate
import com.android.messaging.ui.conversation.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.composer.model.ConversationSubscriptionSelectionState
import com.android.messaging.ui.conversation.focus.delegate.ConversationFocusDelegate
import com.android.messaging.ui.conversation.mediapicker.delegate.ConversationMediaPickerDelegate
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessageSelectionDelegate
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.screen.ConversationViewModel
import com.android.messaging.ui.conversation.screen.model.ConversationAttachmentLimitWarning
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseConversationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected fun createViewModel(
        audioRecordingDelegate: ConversationAudioRecordingDelegate =
            createAudioRecordingDelegateMock().mock,
        composerAttachmentsDelegate: ConversationComposerAttachmentsDelegate =
            createComposerAttachmentsDelegateMock().mock,
        draftDelegate: ConversationDraftDelegate = createDraftDelegateMock().mock,
        messagesDelegate: ConversationMessagesDelegate = createMessagesDelegateMock().mock,
        messageSelectionDelegate: ConversationMessageSelectionDelegate =
            createMessageSelectionDelegateMock().mock,
        mediaPickerDelegate: ConversationMediaPickerDelegate = createMediaPickerDelegateMock().mock,
        metadataDelegate: ConversationMetadataDelegate = createMetadataDelegateMock().mock,
        focusDelegate: ConversationFocusDelegate = createFocusDelegateMock().mock,
        canAddMoreConversationParticipants: CanAddMoreConversationParticipants = mockk {
            every { invoke(participantCount = any()) } returns false
        },
        createDefaultSmsRoleRequest: CreateDefaultSmsRoleRequest = CreateDefaultSmsRoleRequest {
            null
        },
        isDeviceVoiceCapable: IsDeviceVoiceCapable = IsDeviceVoiceCapable { false },
        isEmergencyPhoneNumber: IsEmergencyPhoneNumber = IsEmergencyPhoneNumber { false },
        composerUiStateMapper: ConversationComposerUiStateMapper =
            createComposerUiStateMapperMock(mappedUiState = ConversationComposerUiState()),
        subscriptionSelectionDelegate: ConversationSubscriptionSelectionDelegate =
            createSubscriptionSelectionDelegateMock().mock,
        simSelectionRepository: ConversationSimSelectionRepository = mockk(relaxed = true),
    ): ConversationViewModel {
        return ConversationViewModel(
            conversationAudioRecordingDelegate = audioRecordingDelegate,
            conversationComposerAttachmentsDelegate = composerAttachmentsDelegate,
            conversationDraftDelegate = draftDelegate,
            conversationMessagesDelegate = messagesDelegate,
            conversationMessageSelectionDelegate = messageSelectionDelegate,
            conversationMediaPickerDelegate = mediaPickerDelegate,
            conversationMetadataDelegate = metadataDelegate,
            conversationFocusDelegate = focusDelegate,
            conversationSubscriptionSelectionDelegate = subscriptionSelectionDelegate,
            conversationComposerUiStateMapper = composerUiStateMapper,
            simSelectionRepository = simSelectionRepository,
            canAddMoreConversationParticipants = canAddMoreConversationParticipants,
            createDefaultSmsRoleRequest = createDefaultSmsRoleRequest,
            isDeviceVoiceCapable = isDeviceVoiceCapable,
            isEmergencyPhoneNumber = isEmergencyPhoneNumber,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            savedStateHandle = SavedStateHandle(),
        )
    }

    protected fun createViewModelInStore(
        viewModelStore: ViewModelStore,
        viewModelFactory: () -> ConversationViewModel = { createViewModel() },
    ): ConversationViewModel {
        return ViewModelProvider(
            store = viewModelStore,
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return viewModelFactory() as T
                }
            },
        )[ConversationViewModel::class.java]
    }

    protected fun createOneOnOneMetadataState(
        phoneNumber: String = TEST_CALL_ACTION_PHONE_NUMBER,
    ): ConversationMetadataUiState.Present {
        return ConversationMetadataUiState.Present(
            title = "Alice",
            selfParticipantId = "self-1",
            avatar = ConversationMetadataUiState.Avatar.Single(photoUri = null),
            participantCount = 1,
            otherParticipantDisplayDestination = phoneNumber,
            otherParticipantPhoneNumber = phoneNumber,
            otherParticipantContactLookupKey = null,
            isArchived = false,
            composerAvailability = ConversationComposerAvailability.Editable,
        )
    }

    protected fun createComposerAttachmentsDelegateMock(): ComposerAttachmentsDelegateMock {
        val bindCalls = mutableListOf<ComposerAttachmentsBindCall>()
        val stateFlow = MutableStateFlow<ImmutableList<ComposerAttachmentUiModel>>(
            persistentListOf(),
        )
        val mock = mockk<ConversationComposerAttachmentsDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += ComposerAttachmentsBindCall(
                scope = firstArg(),
                draftStateFlow = secondArg(),
            )
        }
        return ComposerAttachmentsDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            bindCalls = bindCalls,
        )
    }

    protected fun createAudioRecordingDelegateMock(): AudioRecordingDelegateMock {
        val bindCalls = mutableListOf<BindCall>()
        val stateFlow = MutableStateFlow(ConversationAudioRecordingUiState())
        val mock = mockk<ConversationAudioRecordingDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += BindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return AudioRecordingDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            bindCalls = bindCalls,
        )
    }

    protected fun createSubscriptionSelectionDelegateMock(
        state: ConversationSubscriptionSelectionState = ConversationSubscriptionSelectionState(
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = false,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        ),
    ): SubscriptionSelectionDelegateMock {
        val stateFlow = MutableStateFlow(state)
        val mock = mockk<ConversationSubscriptionSelectionDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        return SubscriptionSelectionDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
        )
    }

    protected fun createDraftDelegateMock(): DraftDelegateMock {
        val bindCalls = mutableListOf<BindCall>()
        val stateFlow = MutableStateFlow(ConversationDraftState())
        val attachmentLimitWarningFlow =
            MutableStateFlow<ConversationAttachmentLimitWarning?>(null)
        val effectsFlow = MutableSharedFlow<ConversationScreenEffect>()
        val isSubjectDialogVisibleFlow = MutableStateFlow(false)
        val mock = mockk<ConversationDraftDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every { mock.attachmentLimitWarning } returns attachmentLimitWarningFlow
        every { mock.effects } returns effectsFlow
        every { mock.isSubjectDialogVisible } returns isSubjectDialogVisibleFlow
        every { mock.tryStartAddingAttachment() } returns true
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += BindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return DraftDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            effectsFlow = effectsFlow,
            bindCalls = bindCalls,
        )
    }

    protected fun createMediaPickerDelegateMock(): MediaPickerDelegateMock {
        val bindCalls = mutableListOf<BindCall>()
        val effectsFlow = MutableSharedFlow<ConversationScreenEffect>()
        val photoPickerSourceContentUriByAttachmentContentUriFlow:
            MutableStateFlow<ImmutableMap<String, String>> =
            MutableStateFlow(persistentMapOf<String, String>())
        val mock = mockk<ConversationMediaPickerDelegate>(relaxed = true)
        every { mock.effects } returns effectsFlow
        every {
            mock.photoPickerSourceContentUriByAttachmentContentUri
        } returns photoPickerSourceContentUriByAttachmentContentUriFlow
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += BindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return MediaPickerDelegateMock(
            mock = mock,
            effectsFlow = effectsFlow,
            photoPickerSourceContentUriByAttachmentContentUriFlow =
            photoPickerSourceContentUriByAttachmentContentUriFlow,
            bindCalls = bindCalls,
        )
    }

    protected fun createMessagesDelegateMock(): MessagesDelegateMock {
        val bindCalls = mutableListOf<BindCall>()
        val stateFlow = MutableStateFlow<ConversationMessagesUiState>(
            ConversationMessagesUiState.Loading,
        )
        val mock = mockk<ConversationMessagesDelegate>()
        every { mock.state } returns stateFlow
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += BindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return MessagesDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            bindCalls = bindCalls,
        )
    }

    protected fun createMessageSelectionDelegateMock(): MessageSelectionDelegateMock {
        val bindCalls = mutableListOf<BindCall>()
        val stateFlow = MutableStateFlow(ConversationMessageSelectionUiState())
        val effectsFlow = MutableSharedFlow<ConversationScreenEffect>()
        val mock = mockk<ConversationMessageSelectionDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every { mock.effects } returns effectsFlow
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += BindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return MessageSelectionDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            effectsFlow = effectsFlow,
            bindCalls = bindCalls,
        )
    }

    protected fun createMetadataDelegateMock(): MetadataDelegateMock {
        val bindCalls = mutableListOf<BindCall>()
        val stateFlow = MutableStateFlow<ConversationMetadataUiState>(
            ConversationMetadataUiState.Loading,
        )
        val effectsFlow = MutableSharedFlow<ConversationScreenEffect>()
        val deleteConfirmationVisibleFlow = MutableStateFlow(value = false)
        val mock = mockk<ConversationMetadataDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every { mock.effects } returns effectsFlow
        every {
            mock.isDeleteConversationConfirmationVisible
        } returns deleteConfirmationVisibleFlow
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += BindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return MetadataDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            effectsFlow = effectsFlow,
            deleteConfirmationVisibleFlow = deleteConfirmationVisibleFlow,
            bindCalls = bindCalls,
        )
    }

    protected fun createFocusDelegateMock(): FocusDelegateMock {
        val bindCalls = mutableListOf<FocusBindCall>()
        val mock = mockk<ConversationFocusDelegate>(relaxed = true)
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += FocusBindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return FocusDelegateMock(
            mock = mock,
            bindCalls = bindCalls,
        )
    }

    protected fun createComposerUiStateMapperMock(
        mappedUiState: ConversationComposerUiState,
    ): ConversationComposerUiStateMapper {
        val mapper = mockk<ConversationComposerUiStateMapper>()
        every {
            mapper.map(any(), any(), any(), any(), any(), any(), any())
        } returns mappedUiState
        return mapper
    }

    protected fun createMessageUiModel(): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = MESSAGE_ID,
            conversationId = CONVERSATION_ID,
            text = "Hello",
            parts = persistentListOf(),
            sentTimestamp = 1L,
            receivedTimestamp = 1L,
            displayTimestamp = 1L,
            status = ConversationMessageUiModel.Status.Outgoing.Complete,
            isIncoming = false,
            senderDisplayName = null,
            senderAvatarUri = null,
            senderContactId = 0L,
            senderContactLookupKey = null,
            senderNormalizedDestination = null,
            senderParticipantId = null,
            selfParticipantId = null,
            canClusterWithPrevious = false,
            canClusterWithNext = false,
            canCopyMessageToClipboard = false,
            canDownloadMessage = false,
            canForwardMessage = false,
            canResendMessage = false,
            canSaveAttachments = false,
            mmsDownload = null,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.SMS,
        )
    }

    protected data class BindCall(
        val scope: CoroutineScope,
        val conversationIdFlow: StateFlow<String?>,
    )

    protected data class DraftDelegateMock(
        val mock: ConversationDraftDelegate,
        val stateFlow: MutableStateFlow<ConversationDraftState>,
        val effectsFlow: MutableSharedFlow<ConversationScreenEffect>,
        val bindCalls: List<BindCall>,
    )

    protected data class ComposerAttachmentsBindCall(
        val scope: CoroutineScope,
        val draftStateFlow: StateFlow<ConversationDraftState>,
    )

    protected data class ComposerAttachmentsDelegateMock(
        val mock: ConversationComposerAttachmentsDelegate,
        val stateFlow: MutableStateFlow<ImmutableList<ComposerAttachmentUiModel>>,
        val bindCalls: List<ComposerAttachmentsBindCall>,
    )

    protected data class AudioRecordingDelegateMock(
        val mock: ConversationAudioRecordingDelegate,
        val stateFlow: MutableStateFlow<ConversationAudioRecordingUiState>,
        val bindCalls: List<BindCall>,
    )

    protected data class SubscriptionSelectionDelegateMock(
        val mock: ConversationSubscriptionSelectionDelegate,
        val stateFlow: MutableStateFlow<ConversationSubscriptionSelectionState>,
    )

    protected data class MediaPickerDelegateMock(
        val mock: ConversationMediaPickerDelegate,
        val effectsFlow: MutableSharedFlow<ConversationScreenEffect>,
        val photoPickerSourceContentUriByAttachmentContentUriFlow:
        MutableStateFlow<ImmutableMap<String, String>>,
        val bindCalls: List<BindCall>,
    )

    protected data class MessagesDelegateMock(
        val mock: ConversationMessagesDelegate,
        val stateFlow: MutableStateFlow<ConversationMessagesUiState>,
        val bindCalls: List<BindCall>,
    )

    protected data class MessageSelectionDelegateMock(
        val mock: ConversationMessageSelectionDelegate,
        val stateFlow: MutableStateFlow<ConversationMessageSelectionUiState>,
        val effectsFlow: MutableSharedFlow<ConversationScreenEffect>,
        val bindCalls: List<BindCall>,
    )

    protected data class MetadataDelegateMock(
        val mock: ConversationMetadataDelegate,
        val stateFlow: MutableStateFlow<ConversationMetadataUiState>,
        val effectsFlow: MutableSharedFlow<ConversationScreenEffect>,
        val deleteConfirmationVisibleFlow: MutableStateFlow<Boolean>,
        val bindCalls: List<BindCall>,
    )

    protected data class FocusBindCall(
        val scope: CoroutineScope,
        val conversationIdFlow: StateFlow<String?>,
    )

    protected data class FocusDelegateMock(
        val mock: ConversationFocusDelegate,
        val bindCalls: List<FocusBindCall>,
    )

    protected companion object {
        const val EMERGENCY_PHONE_NUMBER = "911"
        const val MESSAGE_ID = "message-1"
    }
}
