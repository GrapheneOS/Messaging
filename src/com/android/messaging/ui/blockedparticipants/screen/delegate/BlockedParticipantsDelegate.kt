package com.android.messaging.ui.blockedparticipants.screen.delegate

import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.blockedparticipants.usecase.DeleteDirectChats
import com.android.messaging.domain.blockedparticipants.usecase.SetDestinationBlocked
import com.android.messaging.ui.blockedparticipants.common.BlockedParticipantsScreenDelegate
import com.android.messaging.ui.blockedparticipants.screen.mapper.BlockedParticipantsUiStateMapper
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantUiState
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsUiState as State
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface BlockedParticipantsDelegate : BlockedParticipantsScreenDelegate<State> {

    fun toggleSelection(participantId: String)
    fun unblock(normalizedDestination: String)
    suspend fun deleteSelectedChats()
}

internal class BlockedParticipantsDelegateImpl @Inject constructor(
    private val repository: BlockedParticipantsRepository,
    private val mapper: BlockedParticipantsUiStateMapper,
    private val setDestinationBlocked: SetDestinationBlocked,
    private val deleteDirectChats: DeleteDirectChats,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
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
            current.copy(selectedParticipantIds = updated.toPersistentSet())
        }
    }

    override fun unblock(normalizedDestination: String) {
        setDestinationBlocked(
            normalizedDestination = normalizedDestination,
            blocked = false,
        )
    }

    override suspend fun deleteSelectedChats() {
        val destinations = _state.value.destinationsForSelection()

        _state.update {
            it.copy(selectedParticipantIds = persistentSetOf())
        }

        if (destinations.isEmpty()) return

        deleteDirectChats(destinations)
    }

    private fun State.destinationsForSelection(): List<String> {
        return participants.asSequence()
            .filter { it.participantId in selectedParticipantIds }
            .mapNotNull { it.normalizedDestination?.takeIf(String::isNotEmpty) }
            .toList()
    }

    private fun ImmutableSet<String>.retainKnown(
        participants: List<BlockedParticipantUiState>,
    ): ImmutableSet<String> {
        if (isEmpty()) return this

        val knownIds = participants.mapTo(HashSet(participants.size)) {
            it.participantId
        }

        return filter { it in knownIds }.toPersistentSet()
    }
}
