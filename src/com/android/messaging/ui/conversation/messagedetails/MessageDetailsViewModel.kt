package com.android.messaging.ui.conversation.messagedetails

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.appsettings.repository.AppSettingsRepository
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
import kotlinx.coroutines.launch

internal const val MESSAGE_DETAILS_CONVERSATION_ID_ARG = "conversationId"
internal const val MESSAGE_DETAILS_MESSAGE_ID_ARG = "messageId"

internal interface MessageDetailsScreenModel {
    val uiState: StateFlow<State>

    fun onCopy(value: String)
}

@HiltViewModel
internal class MessageDetailsViewModel @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val messageDetailsUiStateMapper: MessageDetailsUiStateMapper,
    private val clipboardManager: ClipboardManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel(),
    MessageDetailsScreenModel {

    private val _uiState = MutableStateFlow<State>(State.Loading)
    override val uiState = _uiState.asStateFlow()

    private val conversationId: ConversationId = requireNotNull(
        ConversationId.fromOrNull(savedStateHandle[MESSAGE_DETAILS_CONVERSATION_ID_ARG]),
    ) { "conversationId is required" }

    private val messageId: MessageId = requireNotNull(
        MessageId.fromOrNull(savedStateHandle[MESSAGE_DETAILS_MESSAGE_ID_ARG]),
    ) { "messageId is required" }

    init {
        bindMessageDetails()
    }

    private fun bindMessageDetails() {
        viewModelScope.launch {
            _uiState.value = loadMessageDetails()
        }
    }

    override fun onCopy(value: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, value))
    }

    private suspend fun loadMessageDetails(): State {
        val result = conversationsRepository.getMessageDetails(
            conversationId = conversationId,
            messageId = messageId,
        )

        return messageDetailsUiStateMapper.map(
            message = result?.message,
            details = result?.details,
            youTubeLinkPreviewsEnabled = appSettingsRepository.isYouTubeLinkPreviewsEnabled(),
        )
    }
}
