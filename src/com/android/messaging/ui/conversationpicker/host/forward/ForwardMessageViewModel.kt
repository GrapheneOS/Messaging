package com.android.messaging.ui.conversationpicker.host.forward

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.domain.conversation.usecase.forward.CreateForwardedMessage
import com.android.messaging.domain.conversationpicker.model.SendContentResult
import com.android.messaging.domain.conversationpicker.model.SendTarget
import com.android.messaging.domain.conversationpicker.usecase.BuildConversationDraftFromMessage
import com.android.messaging.domain.conversationpicker.usecase.SendContentToTargets
import com.android.messaging.ui.conversation.navigation.ConversationDraftLauncher
import com.android.messaging.ui.conversationpicker.host.forward.ForwardMessageNavEvent as NavEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val FORWARD_CONVERSATION_ID_ARG = "conversationId"
internal const val FORWARD_MESSAGE_ID_ARG = "messageId"

@HiltViewModel
internal class ForwardMessageViewModel @Inject constructor(
    @param:ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val conversationDraftLauncher: ConversationDraftLauncher,
    private val sendContentToTargets: SendContentToTargets,
    buildConversationDraftFromMessage: BuildConversationDraftFromMessage,
    createForwardedMessage: CreateForwardedMessage,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val conversationId = requireNotNull(
        ConversationId.fromOrNull(savedStateHandle[FORWARD_CONVERSATION_ID_ARG]),
    ) { "conversationId is required" }

    private val messageId = requireNotNull(
        MessageId.fromOrNull(savedStateHandle[FORWARD_MESSAGE_ID_ARG]),
    ) { "messageId is required" }

    private val _uiState = MutableStateFlow(ForwardMessageUiState())
    val uiState: StateFlow<ForwardMessageUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavEvent> = _navigationEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            val message = createForwardedMessage(
                conversationId = conversationId,
                messageId = messageId,
            )

            _uiState.value = ForwardMessageUiState(
                draft = message?.let(buildConversationDraftFromMessage::invoke),
                message = message,
                isLoading = false,
            )
        }
    }

    fun onTargetSelected(conversationId: ConversationId) {
        conversationDraftLauncher.launch(
            conversationId = conversationId,
            draft = _uiState.value.message,
        )
        _navigationEvents.trySend(NavEvent.OpenConversation(conversationId))
    }

    fun onSendToTargets(
        targets: Set<SendTarget>,
        draft: ConversationDraft,
        onSendFailure: () -> Unit,
    ) {
        applicationScope.launch {
            val result = sendContentToTargets(draft, targets)
            if (result is SendContentResult.Failure) {
                withContext(mainDispatcher) {
                    onSendFailure()
                }
            }
        }
        _navigationEvents.trySend(NavEvent.Close)
    }
}
