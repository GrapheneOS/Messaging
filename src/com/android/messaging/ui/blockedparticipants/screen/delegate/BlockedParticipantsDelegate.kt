package com.android.messaging.ui.blockedparticipants.screen.delegate

import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.blockedparticipants.usecase.DeleteDirectChats
import com.android.messaging.ui.blockedparticipants.common.BlockedParticipantsScreenDelegate
import com.android.messaging.ui.blockedparticipants.screen.mapper.BlockedParticipantsUiStateMapper
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantUiState
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsUiState as State
import javax.inject.Inject
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.plus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface BlockedParticipantsDelegate : BlockedParticipantsScreenDelegate<State> {
    fun toggleSelection(participantId: String)
    fun clearSelection()
    fun unblock(normalizedDestination: String)
    fun deleteSelectedChats()
}

internal class BlockedParticipantsDelegateImpl @Inject constructor(
    private val repository: BlockedParticipantsRepository,
    private val mapper: BlockedParticipantsUiStateMapper,
    private val deleteDirectChats: DeleteDirectChats,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    @param:ApplicationCoroutineScope
    private val applicationScope: CoroutineScope,
) : BlockedParticipantsDelegate {

    private val _state = MutableStateFlow(State())
    override val state: StateFlow<State> = _state.asStateFlow()

    private var isBound = false

    override fun bind(scope: CoroutineScope) {
        if (isBound) return
        isBound = true

        scope.launch(defaultDispatcher) {
            repository.observeBlockedParticipants().collect { participants ->
                val data = mapper.map(participants)
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        participants = data,
                        selectedParticipantIds = current.selectedParticipantIds.retainKnown(data),
                    )
                }
            }
        }
    }

    override fun toggleSelection(participantId: String) {
        _state.update { current ->
            val updated = if (participantId in current.selectedParticipantIds) {
                current.selectedParticipantIds - participantId
            } else {
                current.selectedParticipantIds + participantId
            }
            current.copy(selectedParticipantIds = updated)
        }
    }

    override fun clearSelection() {
        _state.update { current ->
            current.copy(selectedParticipantIds = persistentSetOf())
        }
    }

    override fun unblock(normalizedDestination: String) {
        applicationScope.launch {
            repository.setDestinationBlocked(
                destination = normalizedDestination,
                conversationId = null,
                isBlocked = false,
            )
        }
    }

    override fun deleteSelectedChats() {
        val conversationIds = _state.value.conversationIdsForSelection()

        _state.update { current ->
            current.copy(selectedParticipantIds = persistentSetOf())
        }

        if (conversationIds.isEmpty()) return

        deleteDirectChats(conversationIds)
    }

    private fun State.conversationIdsForSelection(): List<String> {
        return participants.asSequence()
            .filter { it.participantId in selectedParticipantIds }
            .map { it.conversationId }
            .toList()
    }

    private fun PersistentSet<String>.retainKnown(
        participants: List<BlockedParticipantUiState>,
    ): PersistentSet<String> {
        if (isEmpty()) return this

        val knownIds = participants.mapTo(HashSet(participants.size)) {
            it.participantId
        }

        return retainAll(knownIds)
    }
}
