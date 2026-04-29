package com.android.messaging.ui.conversation.v2.screen

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.repository.ConversationSubscriptionsRepository
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.conversation.usecase.action.CreateDefaultSmsRoleRequest
import com.android.messaging.domain.conversation.usecase.participant.CanAddMoreConversationParticipants
import com.android.messaging.domain.conversation.usecase.telephony.IsDeviceVoiceCapable
import com.android.messaging.domain.conversation.usecase.telephony.IsEmergencyPhoneNumber
import com.android.messaging.ui.conversation.v2.audio.delegate.ConversationAudioRecordingDelegate
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationComposerAttachmentsDelegate
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.v2.focus.delegate.ConversationFocusDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationMediaPickerDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessageSelectionDelegate
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMediaPickerOverlayUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenScaffoldUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface ConversationScreenModel {
    val effects: Flow<ConversationScreenEffect>
    val mediaPickerOverlayUiState: StateFlow<ConversationMediaPickerOverlayUiState>
    val scaffoldUiState: StateFlow<ConversationScreenScaffoldUiState>

    fun onConversationIdChanged(conversationId: String?)
    fun onOpenStartupAttachment(
        conversationId: String,
        startupAttachment: ConversationEntryStartupAttachment,
    )

    fun onSeedDraft(
        conversationId: String,
        draft: ConversationDraft,
    )

    fun onAttachmentClicked(
        attachment: ComposerAttachmentUiModel.Resolved,
    )

    fun onMessageAttachmentClicked(
        contentType: String,
        contentUri: String,
    )

    fun onMessageClick(messageId: String)
    fun onMessageLongClick(messageId: String)
    fun onMessageResendClick(messageId: String)
    fun onMessageSelectionActionClick(action: ConversationMessageSelectionAction)

    fun onCallClick()

    fun onSimSelected(selfParticipantId: String)

    fun onExternalUriClicked(uri: String)

    fun onPhotoPickerMediaSelected(contentUris: List<String>)
    fun onPhotoPickerMediaDeselected(contentUris: List<String>)
    fun onContactCardPicked(contactUri: String?)
    fun onMessageTextChanged(text: String)
    fun onAudioRecordingStart()
    fun onLockedAudioRecordingStart()
    fun onAudioRecordingLock(): Boolean
    fun onAudioRecordingFinish()
    fun onAudioRecordingCancel()
    fun onCapturedMediaReady(capturedMedia: ConversationCapturedMedia)
    fun onRemovePendingAttachment(pendingAttachmentId: String)
    fun onRemoveResolvedAttachment(contentUri: String)
    fun onUpdateAttachmentCaption(
        contentUri: String,
        captionText: String,
    )

    fun dismissDeleteMessageConfirmation()
    fun dismissMessageSelection()
    fun confirmDeleteSelectedMessages()
    fun onSendClick()
    fun onDefaultSmsRolePromptActionClick()
    fun onDefaultSmsRoleRequestResult(resultCode: Int)
    fun onDefaultSmsRoleRequestLaunchFailed()
    fun persistDraft()

    fun onArchiveConversationClick()
    fun onUnarchiveConversationClick()
    fun onAddContactClick()
    fun onDeleteConversationClick()
    fun confirmDeleteConversation()
    fun dismissDeleteConversationConfirmation()

    fun onScreenForegrounded(cancelNotification: Boolean)
    fun onScreenBackgrounded()
}

@HiltViewModel
internal class ConversationViewModel @Inject constructor(
    private val conversationAudioRecordingDelegate: ConversationAudioRecordingDelegate,
    private val conversationComposerAttachmentsDelegate: ConversationComposerAttachmentsDelegate,
    private val conversationDraftDelegate: ConversationDraftDelegate,
    private val conversationMessagesDelegate: ConversationMessagesDelegate,
    private val conversationMessageSelectionDelegate: ConversationMessageSelectionDelegate,
    private val conversationMediaPickerDelegate: ConversationMediaPickerDelegate,
    private val conversationMetadataDelegate: ConversationMetadataDelegate,
    private val conversationFocusDelegate: ConversationFocusDelegate,
    private val conversationComposerUiStateMapper: ConversationComposerUiStateMapper,
    private val conversationSubscriptionsRepository: ConversationSubscriptionsRepository,
    private val canAddMoreConversationParticipants: CanAddMoreConversationParticipants,
    private val createDefaultSmsRoleRequest: CreateDefaultSmsRoleRequest,
    private val isDeviceVoiceCapable: IsDeviceVoiceCapable,
    private val isEmergencyPhoneNumber: IsEmergencyPhoneNumber,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(),
    ConversationScreenModel {

    private val conversationIdFlow: StateFlow<String?> = savedStateHandle.getStateFlow(
        key = CONVERSATION_ID_KEY,
        initialValue = null,
    )
    private val _effects = MutableSharedFlow<ConversationScreenEffect>(
        extraBufferCapacity = 1,
    )

    override val effects = _effects.asSharedFlow()

    private val subscriptionsFlow = conversationSubscriptionsRepository
        .observeActiveSubscriptions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
            ),
            initialValue = persistentListOf(),
        )

    init {
        initializeDelegates()
    }

    private val composerUiState = combine(
        conversationAudioRecordingDelegate.state,
        conversationMetadataDelegate.state,
        conversationDraftDelegate.state,
        conversationComposerAttachmentsDelegate.state,
        subscriptionsFlow,
    ) { audioRecordingState, metadataState, draftState, attachments, subscriptions ->
        conversationComposerUiStateMapper.map(
            audioRecording = audioRecordingState,
            draftState = draftState,
            attachments = attachments,
            composerAvailability = metadataState.composerAvailability,
            subscriptions = subscriptions,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = conversationComposerUiStateMapper.map(
            audioRecording = conversationAudioRecordingDelegate.state.value,
            draftState = conversationDraftDelegate.state.value,
            attachments = conversationComposerAttachmentsDelegate.state.value,
            composerAvailability = conversationMetadataDelegate.state.value.composerAvailability,
            subscriptions = subscriptionsFlow.value,
        ),
    )

    override val scaffoldUiState: StateFlow<ConversationScreenScaffoldUiState> = combine(
        conversationMetadataDelegate.state,
        conversationMessagesDelegate.state,
        composerUiState,
        conversationMessageSelectionDelegate.state,
        conversationMetadataDelegate.isDeleteConversationConfirmationVisible,
    ) { metadataState, messagesUiState, composerUiState, selectionUiState, isDeleteConfirmVisible ->
        buildScaffoldUiState(
            metadataState = metadataState,
            messagesUiState = messagesUiState,
            composerUiState = composerUiState,
            selectionUiState = selectionUiState,
            isDeleteConversationConfirmationVisible = isDeleteConfirmVisible,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = buildScaffoldUiState(
            metadataState = conversationMetadataDelegate.state.value,
            messagesUiState = conversationMessagesDelegate.state.value,
            composerUiState = composerUiState.value,
            selectionUiState = conversationMessageSelectionDelegate.state.value,
            isDeleteConversationConfirmationVisible =
                conversationMetadataDelegate.isDeleteConversationConfirmationVisible.value,
        ),
    )

    private fun buildScaffoldUiState(
        metadataState: ConversationMetadataUiState,
        messagesUiState: ConversationMessagesUiState,
        composerUiState: ConversationComposerUiState,
        selectionUiState: ConversationMessageSelectionUiState,
        isDeleteConversationConfirmationVisible: Boolean,
    ): ConversationScreenScaffoldUiState {
        val isPresent = metadataState is ConversationMetadataUiState.Present
        val presentMetadata = metadataState as? ConversationMetadataUiState.Present

        return ConversationScreenScaffoldUiState(
            canAddPeople = canAddPeople(metadataState = metadataState),
            canCall = canCall(metadataState = metadataState),
            canArchive = isPresent && presentMetadata?.isArchived == false,
            canUnarchive = isPresent && presentMetadata?.isArchived == true,
            canAddContact = canAddContact(metadataState = metadataState),
            canDeleteConversation = isPresent,
            isDeleteConversationConfirmationVisible = isDeleteConversationConfirmationVisible,
            metadata = metadataState,
            messages = messagesUiState,
            composer = composerUiState,
            selection = selectionUiState,
        )
    }

    override val mediaPickerOverlayUiState = combine(
        conversationMetadataDelegate.state,
        composerUiState,
        conversationMediaPickerDelegate.photoPickerSourceContentUriByAttachmentContentUri,
    ) { metadataState, composerUiState, photoPickerSourceContentUriByAttachmentContentUri ->
        val conversationTitle = when (metadataState) {
            is ConversationMetadataUiState.Present -> metadataState.title
            else -> null
        }

        ConversationMediaPickerOverlayUiState(
            attachments = composerUiState.attachments,
            conversationTitle = conversationTitle,
            isSendActionEnabled = composerUiState.isSendEnabled,
            photoPickerSourceContentUriByAttachmentContentUri =
                photoPickerSourceContentUriByAttachmentContentUri,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = ConversationMediaPickerOverlayUiState(
            attachments = composerUiState.value.attachments,
            conversationTitle = null,
            isSendActionEnabled = composerUiState.value.isSendEnabled,
            photoPickerSourceContentUriByAttachmentContentUri = conversationMediaPickerDelegate
                .photoPickerSourceContentUriByAttachmentContentUri.value,
        ),
    )

    private fun initializeDelegates() {
        conversationAudioRecordingDelegate.bind(
            scope = viewModelScope,
            conversationIdFlow = conversationIdFlow,
        )
        conversationDraftDelegate.bind(
            scope = viewModelScope,
            conversationIdFlow = conversationIdFlow,
        )
        conversationComposerAttachmentsDelegate.bind(
            scope = viewModelScope,
            draftStateFlow = conversationDraftDelegate.state,
        )
        conversationMediaPickerDelegate.bind(
            scope = viewModelScope,
            conversationIdFlow = conversationIdFlow,
        )
        conversationMessagesDelegate.bind(
            scope = viewModelScope,
            conversationIdFlow = conversationIdFlow,
        )
        conversationMessageSelectionDelegate.bind(
            scope = viewModelScope,
            conversationIdFlow = conversationIdFlow,
        )
        conversationMetadataDelegate.bind(
            scope = viewModelScope,
            conversationIdFlow = conversationIdFlow,
        )
        conversationFocusDelegate.bind(
            scope = viewModelScope,
            conversationIdFlow = conversationIdFlow,
        )
        bindDelegateEffects()
    }

    private fun bindDelegateEffects() {
        viewModelScope.launch(defaultDispatcher) {
            conversationDraftDelegate.effects.collect(_effects::emit)
        }
        viewModelScope.launch(defaultDispatcher) {
            conversationMediaPickerDelegate.effects.collect(_effects::emit)
        }
        viewModelScope.launch(defaultDispatcher) {
            conversationMessageSelectionDelegate.effects.collect(_effects::emit)
        }
        viewModelScope.launch(defaultDispatcher) {
            conversationMetadataDelegate.effects.collect(_effects::emit)
        }
    }

    override fun onConversationIdChanged(conversationId: String?) {
        updateConversationId(conversationId = conversationId)
    }

    private fun updateConversationId(conversationId: String?) {
        if (conversationId != conversationIdFlow.value) {
            conversationMessageSelectionDelegate.dismissMessageSelection()
            savedStateHandle[CONVERSATION_ID_KEY] = conversationId
        }
    }

    private fun canAddPeople(
        metadataState: ConversationMetadataUiState,
    ): Boolean {
        return when (metadataState) {
            is ConversationMetadataUiState.Present -> {
                canAddMoreConversationParticipants(
                    participantCount = metadataState.participantCount,
                )
            }
            else -> false
        }
    }

    private fun canCall(
        metadataState: ConversationMetadataUiState,
    ): Boolean {
        if (metadataState !is ConversationMetadataUiState.Present) {
            return false
        }

        val phoneNumber = metadataState.otherParticipantPhoneNumber
        if (metadataState.participantCount != 1 || phoneNumber == null) {
            return false
        }

        return isDeviceVoiceCapable() && !isEmergencyPhoneNumber(phoneNumber = phoneNumber)
    }

    private fun canAddContact(
        metadataState: ConversationMetadataUiState,
    ): Boolean {
        if (metadataState !is ConversationMetadataUiState.Present) {
            return false
        }

        val hasDestination = !metadataState.otherParticipantPhoneNumber.isNullOrBlank()
        val hasContactLink = !metadataState.otherParticipantContactLookupKey.isNullOrBlank()

        return metadataState.participantCount == 1 &&
            hasDestination &&
            !hasContactLink
    }

    override fun onSeedDraft(
        conversationId: String,
        draft: ConversationDraft,
    ) {
        conversationDraftDelegate.seedDraft(
            conversationId = conversationId,
            draft = draft,
        )
    }

    override fun onOpenStartupAttachment(
        conversationId: String,
        startupAttachment: ConversationEntryStartupAttachment,
    ) {
        val imageCollectionUri = MessagingContentProvider
            .buildConversationImagesUri(conversationId)
            ?.toString()

        viewModelScope.launch(defaultDispatcher) {
            _effects.emit(
                ConversationScreenEffect.OpenAttachmentPreview(
                    contentType = startupAttachment.contentType,
                    contentUri = startupAttachment.contentUri,
                    imageCollectionUri = imageCollectionUri,
                ),
            )
        }
    }

    override fun onAttachmentClicked(attachment: ComposerAttachmentUiModel.Resolved) {
        val imageCollectionUri = conversationIdFlow
            .value
            ?.let(MessagingContentProvider::buildDraftImagesUri)
            ?.toString()

        viewModelScope.launch(defaultDispatcher) {
            _effects.emit(
                ConversationScreenEffect.OpenAttachmentPreview(
                    contentType = attachment.contentType,
                    contentUri = attachment.contentUri,
                    imageCollectionUri = imageCollectionUri,
                ),
            )
        }
    }

    override fun onMessageAttachmentClicked(
        contentType: String,
        contentUri: String,
    ) {
        val imageCollectionUri = conversationIdFlow
            .value
            ?.let(MessagingContentProvider::buildConversationImagesUri)
            ?.toString()

        viewModelScope.launch(defaultDispatcher) {
            _effects.emit(
                ConversationScreenEffect.OpenAttachmentPreview(
                    contentType = contentType,
                    contentUri = contentUri,
                    imageCollectionUri = imageCollectionUri,
                ),
            )
        }
    }

    override fun onMessageClick(messageId: String) {
        conversationMessageSelectionDelegate.onMessageClick(messageId = messageId)
    }

    override fun onMessageLongClick(messageId: String) {
        conversationMessageSelectionDelegate.onMessageLongClick(messageId = messageId)
    }

    override fun onMessageResendClick(messageId: String) {
        conversationMessageSelectionDelegate.onMessageResendClick(messageId = messageId)
    }

    override fun onMessageSelectionActionClick(action: ConversationMessageSelectionAction) {
        conversationMessageSelectionDelegate.onMessageSelectionActionClick(action = action)
    }

    override fun onCallClick() {
        val phoneNumber = (
            conversationMetadataDelegate.state.value as?
                ConversationMetadataUiState.Present
            )
            ?.otherParticipantPhoneNumber
            ?.takeUnless(isEmergencyPhoneNumber::invoke)
            ?: return

        viewModelScope.launch(defaultDispatcher) {
            _effects.emit(
                ConversationScreenEffect.PlacePhoneCall(
                    phoneNumber = phoneNumber,
                ),
            )
        }
    }

    override fun onSimSelected(selfParticipantId: String) {
        conversationDraftDelegate.onSelfParticipantIdChanged(
            selfParticipantId = selfParticipantId,
        )
    }

    override fun onExternalUriClicked(uri: String) {
        viewModelScope.launch(defaultDispatcher) {
            _effects.emit(
                ConversationScreenEffect.OpenExternalUri(
                    uri = uri,
                ),
            )
        }
    }

    override fun onPhotoPickerMediaSelected(contentUris: List<String>) {
        conversationMediaPickerDelegate.onPhotoPickerMediaSelected(contentUris = contentUris)
    }

    override fun onPhotoPickerMediaDeselected(contentUris: List<String>) {
        conversationMediaPickerDelegate.onPhotoPickerMediaDeselected(contentUris = contentUris)
    }

    override fun onContactCardPicked(contactUri: String?) {
        conversationMediaPickerDelegate.onContactCardPicked(contactUri = contactUri)
    }

    override fun onMessageTextChanged(text: String) {
        conversationDraftDelegate.onMessageTextChanged(messageText = text)
    }

    override fun onAudioRecordingStart() {
        startAudioRecording(isLocked = false)
    }

    override fun onLockedAudioRecordingStart() {
        startAudioRecording(isLocked = true)
    }

    private fun startAudioRecording(isLocked: Boolean) {
        val effectiveSelfParticipantId = composerUiState.value
            .simSelector
            .selectedSubscription
            ?.selfParticipantId
            ?: conversationDraftDelegate.state.value.draft.selfParticipantId

        when {
            isLocked -> {
                conversationAudioRecordingDelegate.startLockedRecording(
                    selfParticipantId = effectiveSelfParticipantId,
                )
            }

            else -> {
                conversationAudioRecordingDelegate.startRecording(
                    selfParticipantId = effectiveSelfParticipantId,
                )
            }
        }
    }

    override fun onAudioRecordingLock(): Boolean {
        return conversationAudioRecordingDelegate.lockRecording()
    }

    override fun onAudioRecordingFinish() {
        conversationAudioRecordingDelegate.finishRecording()
    }

    override fun onAudioRecordingCancel() {
        conversationAudioRecordingDelegate.cancelRecording()
    }

    override fun onCapturedMediaReady(capturedMedia: ConversationCapturedMedia) {
        conversationMediaPickerDelegate.onCapturedMediaReady(capturedMedia = capturedMedia)
    }

    override fun onRemovePendingAttachment(pendingAttachmentId: String) {
        conversationMediaPickerDelegate.onRemovePendingAttachment(pendingAttachmentId)
    }

    override fun onRemoveResolvedAttachment(contentUri: String) {
        conversationMediaPickerDelegate.onRemoveResolvedAttachment(contentUri = contentUri)
    }

    override fun onUpdateAttachmentCaption(
        contentUri: String,
        captionText: String,
    ) {
        conversationDraftDelegate.updateAttachmentCaption(
            contentUri = contentUri,
            captionText = captionText,
        )
    }

    override fun dismissDeleteMessageConfirmation() {
        conversationMessageSelectionDelegate.dismissDeleteMessageConfirmation()
    }

    override fun dismissMessageSelection() {
        conversationMessageSelectionDelegate.dismissMessageSelection()
    }

    override fun confirmDeleteSelectedMessages() {
        conversationMessageSelectionDelegate.confirmDeleteSelectedMessages()
    }

    override fun onSendClick() {
        conversationDraftDelegate.onSendClick()
    }

    override fun onDefaultSmsRolePromptActionClick() {
        viewModelScope.launch(defaultDispatcher) {
            when (val requestIntent = createDefaultSmsRoleRequest()) {
                null -> {
                    _effects.emit(
                        ConversationScreenEffect.ShowMessage(
                            messageResId = R.string.activity_not_found_message,
                        ),
                    )
                }

                else -> {
                    _effects.emit(
                        ConversationScreenEffect.LaunchDefaultSmsRoleRequest(
                            intent = requestIntent,
                        ),
                    )
                }
            }
        }
    }

    override fun onDefaultSmsRoleRequestResult(resultCode: Int) {
        if (handlePendingDefaultSmsRoleRequestResult(resultCode = resultCode)) {
            return
        }

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        viewModelScope.launch(defaultDispatcher) {
            _effects.emit(
                ConversationScreenEffect.ShowMessage(
                    messageResId = R.string.toast_after_setting_default_sms_app,
                ),
            )
        }
    }

    private fun handlePendingDefaultSmsRoleRequestResult(resultCode: Int): Boolean {
        val didHandleDraftSend = conversationDraftDelegate.onDefaultSmsRoleRequestResult(
            resultCode = resultCode,
        )

        if (didHandleDraftSend) {
            return true
        }

        return conversationMessageSelectionDelegate.onDefaultSmsRoleRequestResult(
            resultCode = resultCode,
        )
    }

    override fun onDefaultSmsRoleRequestLaunchFailed() {
        viewModelScope.launch(defaultDispatcher) {
            _effects.emit(
                ConversationScreenEffect.ShowMessage(
                    messageResId = R.string.activity_not_found_message,
                ),
            )
        }
    }

    override fun persistDraft() {
        conversationDraftDelegate.persistDraft()
    }

    override fun onArchiveConversationClick() {
        conversationMetadataDelegate.onArchiveConversationClick()
    }

    override fun onUnarchiveConversationClick() {
        conversationMetadataDelegate.onUnarchiveConversationClick()
    }

    override fun onAddContactClick() {
        conversationMetadataDelegate.onAddContactClick()
    }

    override fun onDeleteConversationClick() {
        conversationMetadataDelegate.onDeleteConversationClick()
    }

    override fun confirmDeleteConversation() {
        conversationMetadataDelegate.confirmDeleteConversation()
    }

    override fun dismissDeleteConversationConfirmation() {
        conversationMetadataDelegate.dismissDeleteConversationConfirmation()
    }

    override fun onScreenForegrounded(cancelNotification: Boolean) {
        conversationFocusDelegate.setScreenFocused(
            focused = true,
            cancelNotification = cancelNotification,
        )
    }

    override fun onScreenBackgrounded() {
        conversationFocusDelegate.setScreenFocused(focused = false)
    }

    override fun onCleared() {
        conversationFocusDelegate.setScreenFocused(focused = false)
        conversationAudioRecordingDelegate.onScreenCleared()
        conversationMediaPickerDelegate.onScreenCleared()
        conversationDraftDelegate.flushDraft()

        super.onCleared()
    }

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
