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
        val otherParticipant = participants.singleOrNull()

        val effectiveSelfId = selfIdOverride
            ?.takeIf(String::isNotEmpty)
            ?: data.dbSelfParticipantId

        val selectedSubscription = subscriptions
            .firstOrNull { it.selfParticipantId == effectiveSelfId }
            ?: subscriptions.firstOrNull()

        return ConversationSettingsUiState(
            conversationId = data.conversationId,
            conversationTitle = data.conversationTitle,
            isArchived = data.isArchived,
            isSnoozed = data.isSnoozed,
            participants = participants,
            otherParticipant = otherParticipant,
            selfParticipantId = effectiveSelfId,
            availableSubscriptions = subscriptions,
            selectedSubscription = selectedSubscription,
            isSimSwitchAvailable = subscriptions.size > 1,
            canCall = canCall(
                participant = otherParticipant,
                isVoiceCapable = data.isVoiceCapable,
            ),
            canShowContact = !otherParticipant?.normalizedDestination.isNullOrBlank(),
            isContactSaved = isContactSaved(otherParticipant),
        )
    }

    private fun canCall(
        participant: ParticipantUiState?,
        isVoiceCapable: Boolean,
    ): Boolean {
        val phoneNumber = participant?.normalizedDestination

        return isVoiceCapable &&
            !phoneNumber.isNullOrBlank() &&
            !PhoneNumberUtils.isEmergencyNumber(phoneNumber)
    }

    private fun isContactSaved(participant: ParticipantUiState?): Boolean {
        if (participant == null) return false
        return participant.contactId > 0 && !participant.lookupKey.isNullOrBlank()
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
