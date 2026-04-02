package com.android.messaging.ui.conversation.v2.metadata.delegate

import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.v2.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.v2.metadata.mapper.ConversationMetadataUiStateMapper
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal interface ConversationMetadataDelegate :
    ConversationScreenDelegate<ConversationMetadataUiState>

internal class ConversationMetadataDelegateImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val conversationMetadataUiStateMapper: ConversationMetadataUiStateMapper,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationMetadataDelegate {

    private val _state = MutableStateFlow<ConversationMetadataUiState>(
        value = ConversationMetadataUiState.Loading,
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
                _state.value = ConversationMetadataUiState.Loading

                if (conversationId == null) {
                    return@collectLatest
                }

                conversationsRepository
                    .getConversationMetadata(conversationId = conversationId)
                    .map { metadata ->
                        when {
                            metadata != null -> {
                                conversationMetadataUiStateMapper.map(metadata = metadata)
                            }
                            else -> ConversationMetadataUiState.Unavailable
                        }
                    }
                    .flowOn(defaultDispatcher)
                    .collect { currentMetadataState ->
                        _state.value = currentMetadataState
                    }
            }
        }
    }
}
