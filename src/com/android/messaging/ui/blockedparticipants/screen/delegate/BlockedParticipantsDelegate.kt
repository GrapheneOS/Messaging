package com.android.messaging.ui.blockedparticipants.screen.delegate

import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.ui.blockedparticipants.screen.mapper.BlockedParticipantsUiStateMapper
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsUiState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface BlockedParticipantsDelegate {
    val state: StateFlow<BlockedParticipantsUiState>

    fun bind(scope: CoroutineScope)
}

internal class BlockedParticipantsDelegateImpl @Inject constructor(
    private val repository: BlockedParticipantsRepository,
    private val mapper: BlockedParticipantsUiStateMapper,
) : BlockedParticipantsDelegate {

    private val _state = MutableStateFlow(BlockedParticipantsUiState())
    override val state: StateFlow<BlockedParticipantsUiState> = _state.asStateFlow()

    private var isBound = false

    override fun bind(scope: CoroutineScope) {
        if (isBound) return
        isBound = true
    }
}
