package com.android.messaging.ui.conversationsettings.screen.mapper

import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat.LTR
import com.android.messaging.data.conversationsettings.model.ConversationSettingsData
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

internal interface ConversationSettingsUiStateMapper {
    fun map(
        data: ConversationSettingsData,
        subscriptions: ImmutableList<Subscription> = persistentListOf(),
        selfIdOverride: String? = null,
    ): ConversationSettingsUiState
}

internal class ConversationSettingsUiStateMapperImpl @Inject constructor(
    private val telephonyManager: TelephonyManager,
) : ConversationSettingsUiStateMapper {

    override fun map(
        data: ConversationSettingsData,
        subscriptions: ImmutableList<Subscription>,
        selfIdOverride: String?,
    ): ConversationSettingsUiState {
        val participants = data.participants
            .map { participant ->
                toParticipantUiState(
                    participant = participant,
                    isVoiceCapable = data.isVoiceCapable,
                )
            }
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
            canCall = otherParticipant?.canCall == true,
            canShowContact = !otherParticipant?.normalizedDestination.isNullOrBlank(),
            isContactSaved = otherParticipant?.isContactSaved == true,
        )
    }

    private fun canCall(
        destination: String?,
        isVoiceCapable: Boolean,
    ): Boolean {
        return isVoiceCapable &&
            !destination.isNullOrBlank() &&
            PhoneNumberUtils.isWellFormedSmsAddress(destination) &&
            !telephonyManager.isEmergencyNumber(destination)
    }

    private fun toParticipantUiState(
        participant: ParticipantData,
        isVoiceCapable: Boolean,
    ): ParticipantUiState {
        val bidiFormatter = BidiFormatter.getInstance()
        val fullName = participant.fullName
        val displayName = when {
            fullName.isNullOrEmpty() -> {
                bidiFormatter.unicodeWrap(participant.sendDestination.orEmpty(), LTR)
            }
            else -> fullName
        }
        val details = when {
            fullName.isNullOrEmpty() || participant.isUnknownSender -> null
            else -> participant.sendDestination?.let {
                bidiFormatter.unicodeWrap(it, LTR)
            }
        }
        val isContactSaved = participant.contactId > 0 && !participant.lookupKey.isNullOrBlank()

        return ParticipantUiState(
            avatarUri = participant.profilePhotoUri?.takeIf(String::isNotBlank),
            displayName = displayName,
            details = details,
            contactId = participant.contactId,
            lookupKey = participant.lookupKey,
            normalizedDestination = participant.normalizedDestination,
            isBlocked = participant.isBlocked,
            displayDestination = participant.displayDestination?.let {
                bidiFormatter.unicodeWrap(it, LTR)
            },
            canCall = canCall(
                destination = participant.normalizedDestination,
                isVoiceCapable = isVoiceCapable,
            ),
            isContactSaved = isContactSaved,
        )
    }
}
