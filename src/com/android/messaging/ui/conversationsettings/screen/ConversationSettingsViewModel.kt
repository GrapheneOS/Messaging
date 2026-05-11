package com.android.messaging.ui.conversationsettings.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversationsettings.screen.delegate.ConversationSettingsDelegate
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction as Action
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsScreenEffect as Effect
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState as State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

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
) : ViewModel(),
    ConversationSettingsScreenModel {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    override val uiState: StateFlow<State> = delegate.state

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

            is Action.ParticipantLongPressed -> {
                emitEffect(Effect.CopyToClipboard(action.details))
            }
        }
    }

    private fun emitEffect(effect: Effect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }
}
