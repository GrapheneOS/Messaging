package com.android.messaging.ui.conversation.messagedetails

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.appsettings.repository.AppSettingsRepository
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

    fun onArguments(conversationId: String, messageId: String)

    fun onCopy(value: String)
}

@HiltViewModel
internal class MessageDetailsViewModel @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val messageDetailsUiStateMapper: MessageDetailsUiStateMapper,
    private val clipboardManager: ClipboardManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(),
    MessageDetailsScreenModel {

    private val _uiState = MutableStateFlow<State>(State.Loading)
    override val uiState = _uiState.asStateFlow()

    private val conversationIdFlow: StateFlow<String?> = savedStateHandle.getStateFlow(
        key = CONVERSATION_ID_KEY,
        initialValue = null,
    )
    private val messageIdFlow: StateFlow<String?> = savedStateHandle.getStateFlow(
        key = MESSAGE_ID_KEY,
        initialValue = null,
    )

    init {
        bindMessageDetails()
    }

    private fun bindMessageDetails() {
        viewModelScope.launch {
            combine(
                conversationIdFlow,
                messageIdFlow,
                ::MessageDetailsArguments,
            ).collectLatest { arguments ->
                _uiState.value = loadMessageDetails(arguments)
            }
        }
    }

    override fun onArguments(
        conversationId: String,
        messageId: String,
    ) {
        if (conversationIdFlow.value != conversationId) {
            savedStateHandle[CONVERSATION_ID_KEY] = conversationId
        }
        if (messageIdFlow.value != messageId) {
            savedStateHandle[MESSAGE_ID_KEY] = messageId
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
            isYouTubePreviewEnabled = appSettingsRepository.isYouTubeLinkPreviewsEnabled(),
        )
    }

    private data class MessageDetailsArguments(
        val conversationId: String?,
        val messageId: String?,
    )

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val MESSAGE_ID_KEY = "message_id"
    }
}
