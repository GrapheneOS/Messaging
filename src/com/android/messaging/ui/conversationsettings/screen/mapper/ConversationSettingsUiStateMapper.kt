package com.android.messaging.ui.conversationsettings.screen.mapper

import com.android.messaging.data.conversationsettings.model.ConversationSettingsData
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.domain.conversation.usecase.participant.CanShowOrAddContact
import com.android.messaging.domain.conversation.usecase.participant.IsContactSaved
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
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

internal class ConversationSettingsUiStateMapperImpl @Inject constructor(
    private val canPlacePhoneCall: CanPlacePhoneCall,
    private val canShowOrAddContact: CanShowOrAddContact,
    private val isContactSavedUseCase: IsContactSaved,
) : ConversationSettingsUiStateMapper {

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

        val canShowContact = otherParticipant?.let { participant ->
            canShowOrAddContact(
                isGroup = false,
                contactId = participant.contactId,
                lookupKey = participant.lookupKey,
                destination = participant.normalizedDestination,
            )
        }

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
            canCall = otherParticipant?.canCall == true,
            canShowContact = canShowContact == true,
            isContactSaved = otherParticipant?.isContactSaved == true,
        )
    }

    private fun toParticipantUiState(
        participant: ParticipantData,
    ): ParticipantUiState {
        val fullName = participant.fullName
        val hasFullName = !fullName.isNullOrEmpty()
        val displayName = when {
            hasFullName -> fullName
            else -> participant.sendDestination.orEmpty()
        }
        val details = when {
            hasFullName && !participant.isUnknownSender -> participant.sendDestination
            else -> null
        }
        val canCall = canPlacePhoneCall(participant.normalizedDestination)
        val isContactSaved = isContactSavedUseCase(
            contactId = participant.contactId,
            lookupKey = participant.lookupKey,
        )

        return ParticipantUiState(
            id = participant.id,
            avatarUri = participant.profilePhotoUri?.takeIf(String::isNotBlank),
            displayName = displayName,
            details = details,
            contactId = participant.contactId,
            lookupKey = participant.lookupKey,
            normalizedDestination = participant.normalizedDestination,
            isBlocked = participant.isBlocked,
            displayDestination = participant.displayDestination,
            canCall = canCall,
            isContactSaved = isContactSaved,
            isDisplayNameLtr = !hasFullName,
        )
    }
}
