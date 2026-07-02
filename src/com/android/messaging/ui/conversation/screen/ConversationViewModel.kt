package com.android.messaging.ui.conversation.screen

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.media.model.ConversationCapturedMedia
import com.android.messaging.data.subscription.repository.ConversationSimSelectionRepository
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.conversation.usecase.action.CreateDefaultSmsRoleRequest
import com.android.messaging.domain.conversation.usecase.participant.CanAddContact
import com.android.messaging.domain.conversation.usecase.participant.CanAddMoreConversationParticipants
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
import com.android.messaging.ui.conversation.audio.delegate.ConversationAudioRecordingDelegate
import com.android.messaging.ui.conversation.composer.delegate.ConversationComposerAttachmentsDelegate
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.composer.delegate.ConversationSubscriptionSelectionDelegate
import com.android.messaging.ui.conversation.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.focus.delegate.ConversationFocusDelegate
import com.android.messaging.ui.conversation.mediapicker.delegate.ConversationMediaPickerDelegate
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessageSelectionDelegate
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.screen.model.ConversationAttachmentLimitWarning
import com.android.messaging.ui.conversation.screen.model.ConversationMediaPickerOverlayUiState
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    fun onMessageAvatarClick(messageId: String)
    fun onMessageDownloadClick(messageId: String)
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
    fun tryStartAddingAttachment(): Boolean
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
    fun dismissAttachmentLimitWarning()
    fun sendAnywayAfterAttachmentLimitWarning()
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

    fun onShowSubjectFieldClick()
    fun onSubjectChipClear()
    fun onSubjectDialogConfirm(subjectText: String)
    fun onSubjectDialogDismiss()

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
    private val conversationSubscriptionSelectionDelegate:
    ConversationSubscriptionSelectionDelegate,
    private val conversationComposerUiStateMapper: ConversationComposerUiStateMapper,
    private val simSelectionRepository: ConversationSimSelectionRepository,
    private val canAddMoreConversationParticipants: CanAddMoreConversationParticipants,
    private val canAddContact: CanAddContact,
    private val canPlacePhoneCall: CanPlacePhoneCall,
    private val createDefaultSmsRoleRequest: CreateDefaultSmsRoleRequest,
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

    init {
        initializeDelegates()
    }

    private val composerUiState = combine(
        conversationAudioRecordingDelegate.state,
        conversationMetadataDelegate.state,
        conversationDraftDelegate.state,
        conversationComposerAttachmentsDelegate.state,
        conversationSubscriptionSelectionDelegate.state,
    ) { audioRecordingState, metadataState, draftState, attachments, subscriptionSelectionState ->
        conversationComposerUiStateMapper.map(
            audioRecording = audioRecordingState,
            draftState = draftState,
            attachments = attachments,
            composerAvailability = metadataState.composerAvailability,
            subscriptions = subscriptionSelectionState.subscriptions,
            areSubscriptionsLoaded = subscriptionSelectionState.areSubscriptionsLoaded,
            defaultSmsSubscriptionId = subscriptionSelectionState.defaultSmsSubscriptionId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = run {
            val subscriptionSelectionState = conversationSubscriptionSelectionDelegate.state.value

            conversationComposerUiStateMapper.map(
                audioRecording = conversationAudioRecordingDelegate.state.value,
                draftState = conversationDraftDelegate.state.value,
                attachments = conversationComposerAttachmentsDelegate.state.value,
                composerAvailability = conversationMetadataDelegate
                    .state.value.composerAvailability,
                subscriptions = subscriptionSelectionState.subscriptions,
                areSubscriptionsLoaded = subscriptionSelectionState.areSubscriptionsLoaded,
                defaultSmsSubscriptionId = subscriptionSelectionState.defaultSmsSubscriptionId,
            )
        },
    )

    private val dialogUiState = combine(
        conversationDraftDelegate.attachmentLimitWarning,
        conversationMetadataDelegate.isDeleteConversationConfirmationVisible,
        conversationDraftDelegate.isSubjectDialogVisible,
    ) { attachmentLimitWarning, isDeleteConversationConfirmationVisible, isSubjectDialogVisible ->
        ConversationScreenDialogUiState(
            attachmentLimitWarning = attachmentLimitWarning,
            isDeleteConversationConfirmationVisible = isDeleteConversationConfirmationVisible,
            isSubjectDialogVisible = isSubjectDialogVisible,
        )
    }

    override val scaffoldUiState: StateFlow<ConversationScreenScaffoldUiState> = combine(
        conversationMetadataDelegate.state,
        conversationMessagesDelegate.state,
        composerUiState,
        conversationMessageSelectionDelegate.state,
        dialogUiState,
    ) { metadataState, messagesUiState, composerUiState, selectionUiState, dialogUiState ->
        buildScaffoldUiState(
            metadataState = metadataState,
            messagesUiState = messagesUiState,
            composerUiState = composerUiState,
            selectionUiState = selectionUiState,
            attachmentLimitWarning = dialogUiState.attachmentLimitWarning,
            isDeleteConversationConfirmationVisible = dialogUiState
                .isDeleteConversationConfirmationVisible,
            isSubjectDialogVisible = dialogUiState.isSubjectDialogVisible,
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
            attachmentLimitWarning = conversationDraftDelegate.attachmentLimitWarning.value,
            isDeleteConversationConfirmationVisible =
                conversationMetadataDelegate.isDeleteConversationConfirmationVisible.value,
            isSubjectDialogVisible = conversationDraftDelegate.isSubjectDialogVisible.value,
        ),
    )

    private fun buildScaffoldUiState(
        metadataState: ConversationMetadataUiState,
        messagesUiState: ConversationMessagesUiState,
        composerUiState: ConversationComposerUiState,
        selectionUiState: ConversationMessageSelectionUiState,
        attachmentLimitWarning: ConversationAttachmentLimitWarning?,
        isDeleteConversationConfirmationVisible: Boolean,
        isSubjectDialogVisible: Boolean,
    ): ConversationScreenScaffoldUiState {
        val isPresent = metadataState is ConversationMetadataUiState.Present
        val presentMetadata = metadataState as? ConversationMetadataUiState.Present

        return ConversationScreenScaffoldUiState(
            canAddPeople = canAddPeople(metadataState = metadataState),
            canCall = canCall(metadataState = metadataState),
            canArchive = isPresent && presentMetadata?.isArchived == false,
            canUnarchive = isPresent && presentMetadata?.isArchived == true,
            canAddContact = isAddContactAvailable(metadataState = metadataState),
            canDeleteConversation = isPresent,
            canEditSubject = isPresent,
            attachmentLimitWarning = attachmentLimitWarning,
            isDeleteConversationConfirmationVisible = isDeleteConversationConfirmationVisible,
            isSubjectDialogVisible = isSubjectDialogVisible,
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
        conversationSubscriptionSelectionDelegate.bind(scope = viewModelScope)
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
        return when {
            metadataState !is ConversationMetadataUiState.Present -> false
            canAddMoreConversationParticipants(metadataState.participantCount) -> true
            else -> false
        }
    }

    private fun canCall(
        metadataState: ConversationMetadataUiState,
    ): Boolean {
        return when {
            metadataState !is ConversationMetadataUiState.Present -> false
            metadataState.participantCount != 1 -> false
            else -> canPlacePhoneCall(metadataState.otherParticipantPhoneNumber)
        }
    }

    private fun isAddContactAvailable(
        metadataState: ConversationMetadataUiState,
    ): Boolean {
        return when {
            metadataState !is ConversationMetadataUiState.Present -> false
            else -> canAddContact(
                isGroup = metadataState.participantCount != 1,
                lookupKey = metadataState.otherParticipantContactLookupKey,
                destination = metadataState.otherParticipantPhoneNumber,
            )
        }
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

    override fun onMessageAvatarClick(messageId: String) {
        val message = when (val messagesState = conversationMessagesDelegate.state.value) {
            is ConversationMessagesUiState.Present -> {
                messagesState
                    .messages
                    .firstOrNull { candidate ->
                        candidate.messageId == messageId
                    }
                    ?.takeIf { it.canShowContactCard }
            }

            else -> null
        }

        if (message == null) {
            return
        }

        viewModelScope.launch(defaultDispatcher) {
            _effects.emit(
                ConversationScreenEffect.ShowOrAddParticipantContact(
                    contactId = message.senderContactId,
                    contactLookupKey = message.senderContactLookupKey,
                    avatarUri = message.senderAvatarUri,
                    normalizedDestination = message.senderNormalizedDestination,
                ),
            )
        }
    }

    override fun onMessageDownloadClick(messageId: String) {
        conversationMessageSelectionDelegate.onMessageDownloadClick(messageId = messageId)
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
            ?.takeIf(canPlacePhoneCall::invoke)
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
        if (selfParticipantId.isBlank()) return
        val conversationId = conversationIdFlow.value?.takeIf(String::isNotBlank) ?: return

        conversationDraftDelegate.onSelfParticipantIdChanged(
            conversationId = conversationId,
            selfParticipantId = selfParticipantId,
        )
        simSelectionRepository.setSelectedSelfId(
            conversationId = conversationId,
            selfId = selfParticipantId,
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

    override fun tryStartAddingAttachment(): Boolean {
        return conversationDraftDelegate.tryStartAddingAttachment()
    }

    override fun onAudioRecordingStart() {
        startAudioRecording(isLocked = false)
    }

    override fun onLockedAudioRecordingStart() {
        startAudioRecording(isLocked = true)
    }

    private fun startAudioRecording(isLocked: Boolean) {
        if (!conversationDraftDelegate.tryStartAddingAttachment()) {
            return
        }

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

    override fun dismissAttachmentLimitWarning() {
        conversationDraftDelegate.dismissAttachmentLimitWarning()
    }

    override fun sendAnywayAfterAttachmentLimitWarning() {
        conversationDraftDelegate.sendAnywayAfterAttachmentLimitWarning()
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

    override fun onShowSubjectFieldClick() {
        conversationDraftDelegate.showSubjectDialog()
    }

    override fun onSubjectChipClear() {
        conversationDraftDelegate.onSubjectTextChanged(subjectText = "")
    }

    override fun onSubjectDialogConfirm(subjectText: String) {
        conversationDraftDelegate.confirmSubjectDialog(subjectText = subjectText)
    }

    override fun onSubjectDialogDismiss() {
        conversationDraftDelegate.dismissSubjectDialog()
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

private data class ConversationScreenDialogUiState(
    val attachmentLimitWarning: ConversationAttachmentLimitWarning?,
    val isDeleteConversationConfirmationVisible: Boolean,
    val isSubjectDialogVisible: Boolean,
)
