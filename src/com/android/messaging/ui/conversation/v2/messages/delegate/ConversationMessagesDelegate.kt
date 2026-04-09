package com.android.messaging.ui.conversation.v2.messages.delegate

import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.v2.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.v2.messages.mapper.ConversationMessageUiModelMapper
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal interface ConversationMessagesDelegate :
    ConversationScreenDelegate<ConversationMessagesUiState>

internal class ConversationMessagesDelegateImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val conversationMessageUiModelMapper: ConversationMessageUiModelMapper,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationMessagesDelegate {

    private val _state = MutableStateFlow<ConversationMessagesUiState>(
        value = ConversationMessagesUiState.Loading,
    )

    override val state = _state.asStateFlow()

    private var isBound = false

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        if (isBound) {
            return
        }

        isBound = true

        scope.launch(defaultDispatcher) {
            conversationIdFlow.collectLatest { conversationId ->
                _state.value = ConversationMessagesUiState.Loading

                if (conversationId == null) {
                    return@collectLatest
                }

                conversationsRepository
                    .getConversationMessages(conversationId = conversationId)
                    .map { messages ->
                        ConversationMessagesUiState.Present(
                            messages = messages
                                .asSequence()
                                .map(conversationMessageUiModelMapper::map)
                                .toImmutableList(),
                        )
                    }
                    .flowOn(defaultDispatcher)
                    .collect { currentMessagesUiState ->
                        _state.value = currentMessagesUiState
                    }
            }
        }
    }
}
