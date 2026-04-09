package com.android.messaging.ui.conversation.v2.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerAttachmentUiState
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationMediaPickerDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.v2.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMediaPickerOverlayUiState
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

    fun onConversationChanged(conversationId: String?)
    fun onAttachmentClicked(
        attachment: ConversationComposerAttachmentUiState.Resolved,
    )

    fun onMessageAttachmentClicked(
        contentType: String,
        contentUri: String,
    )

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

    fun onSendClick()
    fun persistDraft()
}

@HiltViewModel
internal class ConversationViewModel @Inject constructor(
    private val conversationDraftDelegate: ConversationDraftDelegate,
    private val conversationMessagesDelegate: ConversationMessagesDelegate,
    private val conversationMediaPickerDelegate: ConversationMediaPickerDelegate,
    private val conversationMetadataDelegate: ConversationMetadataDelegate,
    private val conversationComposerUiStateMapper: ConversationComposerUiStateMapper,
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
    ) { metadataState, messagesUiState, composerUiState ->
        ConversationScreenScaffoldUiState(
            metadata = metadataState,
            messages = messagesUiState,
            composer = composerUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = ConversationScreenScaffoldUiState(
            metadata = conversationMetadataDelegate.state.value,
            messages = conversationMessagesDelegate.state.value,
            composer = composerUiState.value,
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
    }

    override fun onConversationChanged(conversationId: String?) {
        if (conversationId != conversationIdFlow.value) {
            savedStateHandle[CONVERSATION_ID_KEY] = conversationId
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
