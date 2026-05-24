package com.android.messaging.ui.blockedparticipants.screen.support

import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantUiState
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

internal const val PARTICIPANT_ID_1 = "p1"
internal const val PARTICIPANT_ID_2 = "p2"
internal const val PARTICIPANT_ID_3 = "p3"

internal const val CONVERSATION_ID_1 = "c1"
internal const val CONVERSATION_ID_2 = "c2"
internal const val CONVERSATION_ID_3 = "c3"

internal const val DISPLAY_NAME_1 = "Spam Caller"
internal const val DISPLAY_NAME_2 = "Boss"
internal const val DISPLAY_NAME_3 = "Unknown"

internal const val DESTINATION_1 = "+31611111111"
internal const val DESTINATION_2 = "+31622222222"
internal const val DESTINATION_3 = "+31633333333"

internal fun loadedState(
    participants: ImmutableList<BlockedParticipantUiState> = defaultParticipants(),
    selectedIds: ImmutableSet<String> = persistentSetOf(),
): BlockedParticipantsUiState {
    return BlockedParticipantsUiState(
        isLoading = false,
        participants = participants,
        selectedParticipantIds = selectedIds,
    )
}

internal fun emptyState(): BlockedParticipantsUiState {
    return BlockedParticipantsUiState(
        isLoading = false,
        participants = persistentListOf(),
        selectedParticipantIds = persistentSetOf(),
    )
}

internal fun stateWithSelection(
    selectedIds: Set<String>,
): BlockedParticipantsUiState {
    return loadedState(selectedIds = selectedIds.toPersistentSet())
}

internal fun defaultParticipants(): ImmutableList<BlockedParticipantUiState> {
    return listOf(
        participant(
            participantId = PARTICIPANT_ID_1,
            conversationId = CONVERSATION_ID_1,
            displayName = DISPLAY_NAME_1,
            destination = DESTINATION_1,
        ),
        participant(
            participantId = PARTICIPANT_ID_2,
            conversationId = CONVERSATION_ID_2,
            displayName = DISPLAY_NAME_2,
            destination = DESTINATION_2,
        ),
        participant(
            participantId = PARTICIPANT_ID_3,
            conversationId = CONVERSATION_ID_3,
            displayName = DISPLAY_NAME_3,
            destination = DESTINATION_3,
        ),
    ).toPersistentList()
}

internal fun participant(
    participantId: String = PARTICIPANT_ID_1,
    conversationId: String = CONVERSATION_ID_1,
    displayName: String = DISPLAY_NAME_1,
    destination: String? = DESTINATION_1,
    details: String? = destination,
): BlockedParticipantUiState {
    return BlockedParticipantUiState(
        participantId = participantId,
        conversationId = conversationId,
        avatarUri = null,
        displayName = displayName,
        details = details,
        contactId = -1L,
        lookupKey = null,
        normalizedDestination = destination,
        canCall = false,
        isContactSaved = false,
    )
}
