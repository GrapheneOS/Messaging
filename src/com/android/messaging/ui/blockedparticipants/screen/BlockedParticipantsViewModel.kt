package com.android.messaging.ui.blockedparticipants.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.blockedparticipants.screen.delegate.BlockedParticipantsDelegate
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsAction as Action
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsNavEvent as NavEvent
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsScreenEffect as Effect
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsUiState as State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

internal interface BlockedParticipantsScreenModel {
    val effects: Flow<Effect>
    val navigationEvents: Flow<NavEvent>
    val uiState: StateFlow<State>

    fun onAction(action: Action)
}

@HiltViewModel
internal class BlockedParticipantsViewModel @Inject constructor(
    private val delegate: BlockedParticipantsDelegate,
) : ViewModel(),
    BlockedParticipantsScreenModel {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    private val _navigationEvents = MutableSharedFlow<NavEvent>(extraBufferCapacity = 1)
    override val navigationEvents: Flow<NavEvent> = _navigationEvents.asSharedFlow()

    override val uiState: StateFlow<State> = delegate.state

    init {
        delegate.bind(viewModelScope)
    }

    override fun onAction(action: Action) {
    }
}
