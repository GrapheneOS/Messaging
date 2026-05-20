package com.android.messaging.ui.blockedparticipants.screen.mapper

import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList

internal interface BlockedParticipantsUiStateMapper {
    fun map(participants: ImmutableList<ParticipantData>): BlockedParticipantsUiState
}

internal class BlockedParticipantsUiStateMapperImpl @Inject constructor() :
    BlockedParticipantsUiStateMapper {

    override fun map(
        participants: ImmutableList<ParticipantData>,
    ): BlockedParticipantsUiState {
        return BlockedParticipantsUiState(
            isLoading = false,
        )
    }
}
