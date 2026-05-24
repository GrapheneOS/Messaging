package com.android.messaging.ui.blockedparticipants.screen.mapper

import android.telephony.PhoneNumberUtils
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat.LTR
import com.android.messaging.data.blockedparticipants.model.BlockedDirectChat
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantUiState
import com.android.messaging.util.PhoneUtils
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface BlockedParticipantsUiStateMapper {
    fun map(
        chats: ImmutableList<BlockedDirectChat>,
    ): ImmutableList<BlockedParticipantUiState>
}

internal class BlockedParticipantsUiStateMapperImpl @Inject constructor() :
    BlockedParticipantsUiStateMapper {

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
        val formatter = BidiFormatter.getInstance()
        val participant = chat.participant
        val contactName = participant.fullName?.takeIf(String::isNotEmpty)
        val sendDestination = participant.sendDestination.orEmpty()

        val displayName = contactName ?: sendDestination
        val details = when {
            contactName != null && !participant.isUnknownSender -> sendDestination
            else -> null
        }
        val normalizedDestination = participant.normalizedDestination
        val canCall = !normalizedDestination.isNullOrBlank() &&
            PhoneNumberUtils.isWellFormedSmsAddress(normalizedDestination) &&
            PhoneUtils.getDefault().isVoiceCapable
        val isContactSaved = participant.contactId > 0 && !participant.lookupKey.isNullOrBlank()

        return BlockedParticipantUiState(
            participantId = participant.id,
            conversationId = chat.conversationId,
            avatarUri = participant.profilePhotoUri?.takeIf(String::isNotBlank),
            displayName = formatter.unicodeWrap(displayName, LTR),
            details = details?.let { formatter.unicodeWrap(it, LTR) },
            contactId = participant.contactId,
            lookupKey = participant.lookupKey,
            normalizedDestination = normalizedDestination,
            canCall = canCall,
            isContactSaved = isContactSaved,
        )
    }
}
