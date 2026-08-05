package com.android.messaging.ui.blockedparticipants.screen.support

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantUiState
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

internal val PARTICIPANT_ID_1 = ParticipantId("p1")
internal val PARTICIPANT_ID_2 = ParticipantId("p2")
internal val PARTICIPANT_ID_3 = ParticipantId("p3")

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
    selectedIds: PersistentSet<ParticipantId> = persistentSetOf(),
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
    selectedIds: Set<ParticipantId>,
): BlockedParticipantsUiState {
    return loadedState(selectedIds = selectedIds.toPersistentSet())
}

internal fun defaultParticipants(): ImmutableList<BlockedParticipantUiState> {
    return listOf(
        participant(
            participantId = PARTICIPANT_ID_1,
            conversationId = ConversationId(CONVERSATION_ID_1),
            displayName = DISPLAY_NAME_1,
            destination = DESTINATION_1,
        ),
        participant(
            participantId = PARTICIPANT_ID_2,
            conversationId = ConversationId(CONVERSATION_ID_2),
            displayName = DISPLAY_NAME_2,
            destination = DESTINATION_2,
        ),
        participant(
            participantId = PARTICIPANT_ID_3,
            conversationId = ConversationId(CONVERSATION_ID_3),
            displayName = DISPLAY_NAME_3,
            destination = DESTINATION_3,
        ),
    ).toPersistentList()
}

internal fun participant(
    participantId: ParticipantId = PARTICIPANT_ID_1,
    conversationId: ConversationId = ConversationId(CONVERSATION_ID_1),
    displayName: String = DISPLAY_NAME_1,
    destination: String? = DESTINATION_1,
    details: String? = destination,
    canShowContact: Boolean = true,
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
        canShowContact = canShowContact,
        isContactSaved = false,
    )
}
