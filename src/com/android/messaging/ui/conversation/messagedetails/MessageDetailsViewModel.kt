package com.android.messaging.ui.conversation.messagedetails

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.ui.conversation.messagedetails.mapper.MessageDetailsUiStateMapper
import com.android.messaging.ui.conversation.messagedetails.model.MessageDetailsUiState as State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal interface MessageDetailsScreenModel {
    val uiState: StateFlow<State>

    fun onArguments(conversationId: ConversationId, messageId: MessageId)

    fun onCopy(value: String)
}

@HiltViewModel
internal class MessageDetailsViewModel @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val messageDetailsUiStateMapper: MessageDetailsUiStateMapper,
    private val clipboardManager: ClipboardManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(),
    MessageDetailsScreenModel {

    private val _uiState = MutableStateFlow<State>(State.Loading)
    override val uiState = _uiState.asStateFlow()

    private val conversationIdFlow: MutableStateFlow<ConversationId?> = MutableStateFlow(
        ConversationId.fromOrNull(savedStateHandle[CONVERSATION_ID_KEY]),
    )
    private val messageIdFlow: MutableStateFlow<MessageId?> = MutableStateFlow(
        MessageId.fromOrNull(savedStateHandle[MESSAGE_ID_KEY]),
    )

    init {
        bindMessageDetails()
    }

    private fun bindMessageDetails() {
        viewModelScope.launch {
            combine(
                conversationIdFlow,
                messageIdFlow,
            ) { conversationId, messageId ->
                MessageDetailsArguments(
                    conversationId = conversationId,
                    messageId = messageId,
                )
            }.collectLatest { arguments ->
                _uiState.value = loadMessageDetails(arguments)
            }
        }
    }

    override fun onArguments(
        conversationId: ConversationId,
        messageId: MessageId,
    ) {
        if (conversationIdFlow.value != conversationId) {
            conversationIdFlow.value = conversationId
            savedStateHandle[CONVERSATION_ID_KEY] = conversationId.value
        }
        if (messageIdFlow.value != messageId) {
            messageIdFlow.value = messageId
            savedStateHandle[MESSAGE_ID_KEY] = messageId.value
        }
    }

    override fun onCopy(value: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, value))
    }

    private suspend fun loadMessageDetails(arguments: MessageDetailsArguments): State {
        val conversationId = arguments.conversationId
        val messageId = arguments.messageId

        if (conversationId == null || messageId == null) {
            return State.Loading
        }

        val result = conversationsRepository.getMessageDetails(
            conversationId = conversationId,
            messageId = messageId,
        )

        return messageDetailsUiStateMapper.map(
            message = result?.message,
            details = result?.details,
        )
    }

    private data class MessageDetailsArguments(
        val conversationId: ConversationId?,
        val messageId: MessageId?,
    )

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val MESSAGE_ID_KEY = "message_id"
    }
}
