package com.android.messaging.ui.conversation.v2.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftEffect
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.v2.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.v2.screen.model.ConversationUiState
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
    val uiState: StateFlow<ConversationUiState>

    fun onConversationChanged(conversationId: String?)
    fun onMessageTextChanged(text: String)
    fun onAttachmentClick()
    fun onSendClick()
    fun persistDraft()
}

@HiltViewModel
internal class ConversationViewModel @Inject constructor(
    private val conversationDraftDelegate: ConversationDraftDelegate,
    private val conversationMessagesDelegate: ConversationMessagesDelegate,
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

    override val uiState: StateFlow<ConversationUiState> = combine(
        conversationMetadataDelegate.state,
        conversationMessagesDelegate.state,
        conversationDraftDelegate.state,
    ) { metadataState, messagesUiState, draft ->
        return@combine ConversationUiState(
            metadata = metadataState,
            messages = messagesUiState,
            composer = conversationComposerUiStateMapper.map(
                draft = draft,
                composerAvailability = metadataState.composerAvailability,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = ConversationUiState(),
    )

    init {
        initializeDelegates()
        bindDelegateEffects()
    }

    private fun initializeDelegates() {
        conversationDraftDelegate.bind(
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
    }

    override fun onConversationChanged(conversationId: String?) {
        if (conversationId != conversationIdFlow.value) {
            savedStateHandle[CONVERSATION_ID_KEY] = conversationId
        }
    }

    override fun onMessageTextChanged(text: String) {
        conversationDraftDelegate.onMessageTextChanged(messageText = text)
    }

    override fun onAttachmentClick() {
        conversationDraftDelegate.onAttachmentClick()
    }

    override fun onSendClick() {
        conversationDraftDelegate.onSendClick()
    }

    override fun persistDraft() {
        conversationDraftDelegate.persistDraft()
    }

    override fun onCleared() {
        conversationDraftDelegate.flushDraft()

        super.onCleared()
    }

    private fun bindDelegateEffects() {
        viewModelScope.launch(defaultDispatcher) {
            conversationDraftDelegate.effects.collect { effect ->
                when (effect) {
                    is ConversationDraftEffect.LaunchAttachmentChooser -> {
                        _effects.emit(
                            ConversationScreenEffect.LaunchAttachmentChooser(
                                conversationId = effect.conversationId,
                            ),
                        )
                    }
                }
            }
        }
    }

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
