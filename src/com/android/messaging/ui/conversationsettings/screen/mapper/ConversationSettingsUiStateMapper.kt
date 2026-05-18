package com.android.messaging.ui.conversationsettings.screen.mapper

import android.telephony.PhoneNumberUtils
import com.android.messaging.data.conversationsettings.model.ConversationSettingsData
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationSettingsUiStateMapper {
    fun map(
        data: ConversationSettingsData,
        subscriptions: ImmutableList<Subscription> = persistentListOf(),
        selfIdOverride: String? = null,
    ): ConversationSettingsUiState
}

internal class ConversationSettingsUiStateMapperImpl @Inject constructor() :
    ConversationSettingsUiStateMapper {

    override fun map(
        data: ConversationSettingsData,
        subscriptions: ImmutableList<Subscription>,
        selfIdOverride: String?,
    ): ConversationSettingsUiState {
        val participants = data.participants
            .map(::toParticipantUiState)
            .toImmutableList()

        val effectiveSelfId = selfIdOverride
            ?.takeIf(String::isNotEmpty)
            ?: data.dbSelfParticipantId

        return ConversationSettingsUiState(
            conversationId = data.conversationId,
            conversationTitle = data.conversationTitle,
            isArchived = data.isArchived,
            isSnoozed = data.isSnoozed,
            participants = participants,
            selfParticipantId = effectiveSelfId,
            availableSubscriptions = subscriptions,
            canCall = canCall(
                participant = participants.singleOrNull(),
                isVoiceCapable = data.isVoiceCapable,
            ),
        )
    }

    private fun canCall(
        participant: ParticipantUiState?,
        isVoiceCapable: Boolean,
    ): Boolean {
        if (!isVoiceCapable) return false

        val phoneNumber = participant?.normalizedDestination
            ?.takeIf { it.isNotBlank() }
            ?: return false

        return !PhoneNumberUtils.isEmergencyNumber(phoneNumber)
    }

    private fun toParticipantUiState(participant: ParticipantData): ParticipantUiState {
        val fullName = participant.fullName
        val displayName = when {
            fullName.isNullOrEmpty() -> participant.sendDestination.orEmpty()
            else -> fullName
        }
        val details = when {
            fullName.isNullOrEmpty() || participant.isUnknownSender -> null
            else -> participant.sendDestination
        }

        return ParticipantUiState(
            participantId = participant.id,
            avatarUri = participant.profilePhotoUri?.takeIf(String::isNotBlank),
            displayName = displayName,
            details = details,
            contactId = participant.contactId,
            lookupKey = participant.lookupKey,
            normalizedDestination = participant.normalizedDestination,
            isBlocked = participant.isBlocked,
            displayDestination = participant.displayDestination,
        )
    }
}
