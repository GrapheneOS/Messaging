package com.android.messaging.ui.blockedparticipants.screen.mapper

import com.android.messaging.data.blockedparticipants.model.BlockedDirectChat
import com.android.messaging.domain.conversation.usecase.participant.CanShowOrAddContact
import com.android.messaging.domain.conversation.usecase.participant.IsContactSaved
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface BlockedParticipantsUiStateMapper {
    fun map(
        chats: ImmutableList<BlockedDirectChat>,
    ): ImmutableList<BlockedParticipantUiState>
}

internal class BlockedParticipantsUiStateMapperImpl @Inject constructor(
    private val canPlacePhoneCall: CanPlacePhoneCall,
    private val canShowOrAddContact: CanShowOrAddContact,
    private val isContactSavedUseCase: IsContactSaved,
) : BlockedParticipantsUiStateMapper {

    override fun map(
        chats: ImmutableList<BlockedDirectChat>,
    ): ImmutableList<BlockedParticipantUiState> {
        return chats
            .map(::toBlockedParticipantUiState)
            .toImmutableList()
    }

    private fun toBlockedParticipantUiState(
        chat: BlockedDirectChat,
    ): BlockedParticipantUiState {
        val participant = chat.participant
        val contactName = participant.fullName?.takeIf(String::isNotEmpty)
        val sendDestination = participant.sendDestination.orEmpty()

        val displayName = contactName ?: sendDestination
        val details = when {
            contactName != null && !participant.isUnknownSender -> sendDestination
            else -> null
        }

        val normalizedDestination = participant.normalizedDestination
        val canCall = canPlacePhoneCall(normalizedDestination)
        val canShowContact = canShowOrAddContact(
            isGroup = false,
            contactId = participant.contactId,
            lookupKey = participant.lookupKey,
            destination = normalizedDestination,
        )
        val isContactSaved = isContactSavedUseCase(
            contactId = participant.contactId,
            lookupKey = participant.lookupKey,
        )

        return BlockedParticipantUiState(
            participantId = participant.id,
            conversationId = chat.conversationId,
            avatarUri = participant.profilePhotoUri?.takeIf(String::isNotBlank),
            displayName = displayName,
            details = details,
            contactId = participant.contactId,
            lookupKey = participant.lookupKey,
            normalizedDestination = normalizedDestination,
            canCall = canCall,
            canShowContact = canShowContact,
            isContactSaved = isContactSaved,
        )
    }
}
