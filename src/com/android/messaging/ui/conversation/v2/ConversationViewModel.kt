package com.android.messaging.ui.conversation.v2.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.v2.messages.mapper.ConversationMessageUiModelMapper
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.metadata.mapper.ConversationMetadataUiStateMapper
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class ConversationViewModel @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val conversationMetadataUiStateMapper: ConversationMetadataUiStateMapper,
    private val conversationMessageUiModelMapper: ConversationMessageUiModelMapper,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val conversationIdFlow: StateFlow<String?> = savedStateHandle.getStateFlow(
        key = CONVERSATION_ID_KEY,
        initialValue = null,
    )

    val uiState: StateFlow<ConversationUiState> = conversationIdFlow
        .flatMapLatest { conversationId ->
            observeConversationUiState(conversationId = conversationId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
            ),
            initialValue = ConversationUiState(),
        )

    var conversationId: String?
        get() = conversationIdFlow.value
        set(value) {
            if (value != conversationIdFlow.value) {
                savedStateHandle[CONVERSATION_ID_KEY] = value
            }
        }

    private fun observeConversationUiState(conversationId: String?): Flow<ConversationUiState> {
        if (conversationId == null) {
            return flowOf(ConversationUiState())
        }

        return combine(
            observeConversationMetadataUiState(conversationId = conversationId),
            observeConversationMessagesUiState(conversationId = conversationId),
        ) { metadata, messages ->
            ConversationUiState(
                metadata = metadata,
                messages = messages,
            )
        }.onStart {
            emit(ConversationUiState())
        }
    }

    private fun observeConversationMetadataUiState(
        conversationId: String,
    ): Flow<ConversationMetadataUiState> {
        return conversationsRepository
            .getConversationMetadata(conversationId = conversationId)
            .map { metadata ->
                metadata
                    ?.let(conversationMetadataUiStateMapper::map)
                    ?: ConversationMetadataUiState.Present()
            }
    }

    private fun observeConversationMessagesUiState(
        conversationId: String,
    ): Flow<ConversationMessagesUiState> {
        return conversationsRepository
            .getConversationMessages(conversationId = conversationId)
            .map { messages ->
                ConversationMessagesUiState.Present(
                    messages = messages.mapNotNull(conversationMessageUiModelMapper::map),
                )
            }
            .flowOn(defaultDispatcher)
    }

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
