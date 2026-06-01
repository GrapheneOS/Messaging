package com.android.messaging.ui.shareintent.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.shareintent.screen.delegate.ShareIntentScreenDelegate
import com.android.messaging.ui.shareintent.screen.model.ShareIntentAction as Action
import com.android.messaging.ui.shareintent.screen.model.ShareIntentScreenEffect as Effect
import com.android.messaging.ui.shareintent.screen.model.ShareIntentUiState as State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn

internal interface ShareIntentScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<State>

    fun onAction(action: Action)
}

@HiltViewModel
internal class ShareIntentViewModel @Inject constructor(
    private val delegate: ShareIntentScreenDelegate,
) : ViewModel(),
    ShareIntentScreenModel {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    override val uiState: StateFlow<State> = delegate.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = State(),
    )

    init {
        delegate.bind(viewModelScope)
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.TargetClicked -> {
                _effects.tryEmit(Effect.OpenConversation(action.conversationId))
            }

            Action.NewMessageClicked -> {
                _effects.tryEmit(Effect.CreateNewConversation)
            }

            Action.SearchOpened -> {
                delegate.setSearchActive(true)
            }

            Action.SearchClosed -> {
                delegate.setSearchActive(false)
            }

            is Action.SearchQueryChanged -> {
                delegate.setSearchQuery(action.query)
            }
        }
    }

    private companion object {
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
