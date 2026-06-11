package com.android.messaging.ui.blockedparticipants.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.blockedparticipants.screen.delegate.BlockedParticipantsDelegate
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantUiState
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
        when (action) {
            is Action.UnblockClicked -> {
                handleUnblockClicked(action.normalizedDestination)
            }

            is Action.ParticipantClicked -> {
                handleParticipantClicked(action.participantId)
            }

            is Action.ParticipantLongClicked -> {
                delegate.toggleSelection(action.participantId)
            }

            is Action.ParticipantMessageClicked -> {
                emitEffect(Effect.OpenParticipantChat(action.conversationId))
            }

            is Action.ParticipantCallClicked -> {
                emitEffect(Effect.PlacePhoneCall(action.destination))
            }

            is Action.ParticipantContactInfoClicked -> {
                showOrAddContact(action.participant)
            }

            Action.DeleteSelectedConfirmed -> {
                delegate.deleteSelectedChats()
            }

            Action.ClearSelectionClicked -> {
                delegate.clearSelection()
            }
        }
    }

    private fun handleUnblockClicked(normalizedDestination: String) {
        if (normalizedDestination.isEmpty()) return

        val wasLast = uiState.value.participants.size == 1
        delegate.unblock(normalizedDestination)

        if (wasLast) {
            emitNavigationEvent(NavEvent.CloseAfterLastUnblock)
        }
    }

    private fun handleParticipantClicked(participantId: String) {
        val state = uiState.value

        if (state.selectedParticipantIds.isNotEmpty()) {
            delegate.toggleSelection(participantId)
            return
        }

        val conversationId = state.participants
            .firstOrNull { it.participantId == participantId }
            ?.conversationId
            ?: return

        emitEffect(Effect.OpenParticipantChat(conversationId))
    }

    private fun showOrAddContact(participant: BlockedParticipantUiState) {
        emitEffect(
            Effect.ShowOrAddContact(
                contactId = participant.contactId,
                contactLookupKey = participant.lookupKey,
                avatarUri = participant.avatarUri,
                normalizedDestination = participant.normalizedDestination,
            ),
        )
    }

    private fun emitEffect(effect: Effect) {
        _effects.tryEmit(effect)
    }

    private fun emitNavigationEvent(event: NavEvent) {
        _navigationEvents.tryEmit(event)
    }
}
