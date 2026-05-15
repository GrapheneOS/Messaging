package com.android.messaging.ui.conversationsettings.screen.mapper

import android.content.ContentResolver
import android.telephony.PhoneNumberUtils
import com.android.messaging.data.conversation.repository.ConversationNotificationRepository
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.datamodel.data.PeopleOptionsItemData
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import com.android.messaging.util.PhoneUtils
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationSettingsUiStateMapper {
    fun map(
        conversationId: String,
        subscriptions: ImmutableList<Subscription> = persistentListOf(),
        selfIdOverride: String? = null,
    ): ConversationSettingsUiState
}

internal class ConversationSettingsUiStateMapperImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val notificationRepository: ConversationNotificationRepository,
) : ConversationSettingsUiStateMapper {

    override fun map(
        conversationId: String,
        subscriptions: ImmutableList<Subscription>,
        selfIdOverride: String?,
    ): ConversationSettingsUiState {
        val isSnoozed = notificationRepository.isSnoozed(conversationId)
        val participantsData = ConversationParticipantsData().apply {
            contentResolver.query(
                MessagingContentProvider.buildConversationParticipantsUri(conversationId),
                ParticipantData.ParticipantsQuery.PROJECTION,
                null,
                null,
                null,
            )?.use { bind(it) }
        }

        val participants = participantsData
            .filter { !it.isSelf }
            .map(::toParticipantUiState)
            .toImmutableList()

        val canCall = participants.singleOrNull()?.let(::canCall) ?: false

        val metadataCursor = contentResolver.query(
            MessagingContentProvider.buildConversationMetadataUri(conversationId),
            PeopleOptionsItemData.PROJECTION,
            null,
            null,
            null,
        )

        return metadataCursor.use { cursor ->
            if (cursor == null || !cursor.moveToFirst()) {
                ConversationSettingsUiState(
                    conversationId = conversationId,
                    isSnoozed = isSnoozed,
                    participants = participants,
                    selfParticipantId = selfIdOverride.orEmpty(),
                    availableSubscriptions = subscriptions,
                    canCall = canCall,
                )
            } else {
                val dbSelfId = cursor.getString(
                    PeopleOptionsItemData.INDEX_CURRENT_SELF_ID,
                ).orEmpty()
                val effectiveSelfId = selfIdOverride
                    ?.takeIf { it.isNotEmpty() }
                    ?: dbSelfId

                ConversationSettingsUiState(
                    conversationId = conversationId,
                    conversationTitle = cursor.getString(
                        PeopleOptionsItemData.INDEX_CONVERSATION_NAME,
                    ).orEmpty(),
                    isArchived = cursor.getInt(
                        PeopleOptionsItemData.INDEX_ARCHIVE_STATUS,
                    ) == 1,
                    isSnoozed = isSnoozed,
                    participants = participants,
                    selfParticipantId = effectiveSelfId,
                    availableSubscriptions = subscriptions,
                    canCall = canCall,
                )
            }
        }
    }

    private fun canCall(participant: ParticipantUiState): Boolean {
        val phoneNumber = participant.normalizedDestination?.takeIf { it.isNotBlank() }
            ?: return false
        if (!PhoneUtils.getDefault().isVoiceCapable) return false
        if (PhoneNumberUtils.isEmergencyNumber(phoneNumber)) return false
        return true
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
            avatarUri = participant.profilePhotoUri?.takeIf { it.isNotBlank() },
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
