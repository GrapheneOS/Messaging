package com.android.messaging.ui.conversation.v2.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.conversation.usecase.CanAddMoreConversationParticipants
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerAttachmentUiState
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationMediaPickerDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessageSelectionDelegate
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.v2.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMediaPickerOverlayUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenScaffoldUiState
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
        attachment: ConversationComposerAttachmentUiState.Resolved,
    )

    fun onMessageAttachmentClicked(
        contentType: String,
        contentUri: String,
    )

    fun onMessageClick(messageId: String)
    fun onMessageLongClick(messageId: String)
    fun onMessageSelectionActionClick(action: ConversationMessageSelectionAction)

    fun onExternalUriClicked(uri: String)

    fun onGalleryMediaConfirmed(mediaItems: List<ConversationMediaItem>)
    fun onMessageTextChanged(text: String)
    fun onGalleryVisibilityChanged(isVisible: Boolean)
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
    fun persistDraft()
}

@HiltViewModel
internal class ConversationViewModel @Inject constructor(
    private val conversationDraftDelegate: ConversationDraftDelegate,
    private val conversationMessagesDelegate: ConversationMessagesDelegate,
    private val conversationMessageSelectionDelegate: ConversationMessageSelectionDelegate,
    private val conversationMediaPickerDelegate: ConversationMediaPickerDelegate,
    private val conversationMetadataDelegate: ConversationMetadataDelegate,
    private val conversationComposerUiStateMapper: ConversationComposerUiStateMapper,
    private val canAddMoreConversationParticipants: CanAddMoreConversationParticipants,
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

    private val composerUiState = combine(
        conversationMetadataDelegate.state,
        conversationDraftDelegate.state,
    ) { metadataState, draftState ->
        conversationComposerUiStateMapper.map(
            draftState = draftState,
            composerAvailability = metadataState.composerAvailability,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = conversationComposerUiStateMapper.map(
            draftState = conversationDraftDelegate.state.value,
            composerAvailability = conversationMetadataDelegate.state.value.composerAvailability,
        ),
    )

    override val scaffoldUiState: StateFlow<ConversationScreenScaffoldUiState> = combine(
        conversationMetadataDelegate.state,
        conversationMessagesDelegate.state,
        composerUiState,
        conversationMessageSelectionDelegate.state,
    ) { metadataState, messagesUiState, composerUiState, selectionUiState ->
        ConversationScreenScaffoldUiState(
            canAddPeople = canAddPeople(metadataState = metadataState),
            metadata = metadataState,
            messages = messagesUiState,
            composer = composerUiState,
            selection = selectionUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = ConversationScreenScaffoldUiState(
            canAddPeople = canAddPeople(
                metadataState = conversationMetadataDelegate.state.value,
            ),
            metadata = conversationMetadataDelegate.state.value,
            messages = conversationMessagesDelegate.state.value,
            composer = composerUiState.value,
            selection = conversationMessageSelectionDelegate.state.value,
        ),
    )

    override val mediaPickerOverlayUiState: StateFlow<ConversationMediaPickerOverlayUiState> =
        combine(
            conversationMetadataDelegate.state,
            conversationMediaPickerDelegate.state,
            composerUiState,
        ) { metadataState, mediaPickerUiState, composerUiState ->
            val conversationTitle = when (metadataState) {
                is ConversationMetadataUiState.Present -> metadataState.title
                else -> null
            }

            ConversationMediaPickerOverlayUiState(
                mediaPicker = mediaPickerUiState,
                attachments = composerUiState.attachments,
                conversationTitle = conversationTitle,
                isSendActionEnabled = composerUiState.isSendEnabled,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
            ),
            initialValue = ConversationMediaPickerOverlayUiState(
                mediaPicker = conversationMediaPickerDelegate.state.value,
                attachments = composerUiState.value.attachments,
                conversationTitle = null,
                isSendActionEnabled = composerUiState.value.isSendEnabled,
            ),
        )

    init {
        initializeDelegates()
    }

    private fun initializeDelegates() {
        conversationDraftDelegate.bind(
            scope = viewModelScope,
            conversationIdFlow = conversationIdFlow,
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
        bindDelegateEffects()
    }

    private fun bindDelegateEffects() {
        viewModelScope.launch(defaultDispatcher) {
            conversationMediaPickerDelegate.effects.collect(_effects::emit)
        }
        viewModelScope.launch(defaultDispatcher) {
            conversationMessageSelectionDelegate.effects.collect(_effects::emit)
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

    override fun onAttachmentClicked(
        attachment: ConversationComposerAttachmentUiState.Resolved,
    ) {
        val conversationId = conversationIdFlow.value

        val imageCollectionUri = conversationId
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
        val conversationId = conversationIdFlow.value

        val imageCollectionUri = conversationId
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

    override fun onMessageSelectionActionClick(action: ConversationMessageSelectionAction) {
        conversationMessageSelectionDelegate.onMessageSelectionActionClick(action = action)
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

    override fun onGalleryMediaConfirmed(mediaItems: List<ConversationMediaItem>) {
        conversationMediaPickerDelegate.onGalleryMediaConfirmed(mediaItems = mediaItems)
    }

    override fun onMessageTextChanged(text: String) {
        conversationDraftDelegate.onMessageTextChanged(messageText = text)
    }

    override fun onGalleryVisibilityChanged(isVisible: Boolean) {
        conversationMediaPickerDelegate.onGalleryVisibilityChanged(isVisible = isVisible)
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

    override fun persistDraft() {
        conversationDraftDelegate.persistDraft()
    }

    override fun onCleared() {
        conversationMediaPickerDelegate.onScreenCleared()
        conversationDraftDelegate.flushDraft()

        super.onCleared()
    }

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
