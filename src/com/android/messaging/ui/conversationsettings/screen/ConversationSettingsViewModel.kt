package com.android.messaging.ui.conversationsettings.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.domain.conversation.usecase.participant.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveConversationIdResult
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversationsettings.screen.delegate.ConversationSettingsDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction as Action
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsScreenEffect as Effect
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState as State

internal interface ConversationSettingsScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<State>

    fun refreshState()
    fun onAction(action: Action)
}

@HiltViewModel
internal class ConversationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val delegate: ConversationSettingsDelegate,
    private val resolveConversationId: ResolveConversationId,
    @param:MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
) : ViewModel(),
    ConversationSettingsScreenModel {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    override val uiState: StateFlow<State> = delegate.state

    private var resolveConversationJob: Job? = null

    init {
        val conversationId: String = requireNotNull(
            savedStateHandle[UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID],
        ) { "conversationId is required" }
        delegate.setConversationId(conversationId)
        delegate.bind(scope = viewModelScope)
    }

    override fun refreshState() {
        delegate.refresh()
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.NotificationsClicked -> {
                val state = uiState.value
                emitEffect(
                    Effect.OpenNotificationChannelSettings(
                        conversationId = state.conversationId,
                        conversationTitle = state.conversationTitle,
                        legacyNotificationEnabled = state.legacyNotificationEnabled,
                        legacyRingtoneString = state.legacyRingtoneString,
                        legacyVibrationEnabled = state.legacyVibrationEnabled,
                    ),
                )
            }

            is Action.UnblockClicked -> {
                delegate.setDestinationBlocked(false)
            }

            is Action.BlockConfirmed -> {
                delegate.setDestinationBlocked(true)
                emitEffect(Effect.FinishAfterBlock)
            }

            is Action.ParticipantPressed -> {
                resolveConversation(
                    destination = action.destination,
                    shouldOpenChat = true,
                )
            }

            is Action.ParticipantLongPressed -> {
                emitEffect(Effect.CopyToClipboard(action.details))
            }

            is Action.ParticipantActionPressed -> {
                resolveConversation(
                    destination = action.destination,
                    shouldOpenChat = false,
                )
            }
        }
    }

    private fun resolveConversation(
        destination: String,
        shouldOpenChat: Boolean,
    ) {
        resolveConversationJob = viewModelScope.launch(mainDispatcher) {
            try {
                val result = resolveConversationId.invoke(listOf(destination))
                handleResolveConversationIdResult(result, shouldOpenChat)
            } finally {
                resolveConversationJob = null
            }
        }
    }

    private fun handleResolveConversationIdResult(
        result: ResolveConversationIdResult,
        shouldOpenChat: Boolean,
    ) {
        when (result) {
            is ResolveConversationIdResult.Resolved -> {
                when {
                    shouldOpenChat -> emitEffect(Effect.OpenParticipantChat(result.conversationId))
                    else -> emitEffect(Effect.OpenParticipantInfo(result.conversationId))
                }
            }

            ResolveConversationIdResult.EmptyDestinations,
            ResolveConversationIdResult.NotResolved,
                -> {
                // TODO: Consider how to handle these states
            }
        }
    }

    private fun emitEffect(effect: Effect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }
}
