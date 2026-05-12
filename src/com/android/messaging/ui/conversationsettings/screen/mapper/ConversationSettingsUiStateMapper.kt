package com.android.messaging.ui.conversationsettings.screen.mapper

import android.content.ContentResolver
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.datamodel.data.PeopleOptionsItemData
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationSettingsUiStateMapper {
    fun map(conversationId: String): ConversationSettingsUiState
}

internal class ConversationSettingsUiStateMapperImpl @Inject constructor(
    private val contentResolver: ContentResolver,
) : ConversationSettingsUiStateMapper {

    override fun map(conversationId: String): ConversationSettingsUiState {
        val participantsData = ConversationParticipantsData().apply {
            contentResolver.query(
                MessagingContentProvider.buildConversationParticipantsUri(conversationId),
                ParticipantData.ParticipantsQuery.PROJECTION,
                null,
                null,
                null,
            )?.use { bind(it) }
        }

        val participantsExcludingSelf = participantsData.filter { !it.isSelf }
        val otherParticipant = participantsData.otherParticipant

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
                    participants = participantsExcludingSelf.map(::toParticipantUiState)
                        .toImmutableList(),
                    otherParticipant = otherParticipant?.let(::toParticipantUiState),
                )
            } else {
                // TODO: Get rid of legacy parameters
                ConversationSettingsUiState(
                    conversationId = conversationId,
                    conversationTitle = cursor.getString(
                        PeopleOptionsItemData.INDEX_CONVERSATION_NAME,
                    ).orEmpty(),
                    legacyNotificationEnabled = cursor.getInt(
                        PeopleOptionsItemData.INDEX_NOTIFICATION_ENABLED,
                    ) == 1,
                    legacyRingtoneString = cursor.getString(
                        PeopleOptionsItemData.INDEX_NOTIFICATION_SOUND_URI,
                    ),
                    legacyVibrationEnabled = cursor.getInt(
                        PeopleOptionsItemData.INDEX_NOTIFICATION_VIBRATION,
                    ) == 1,
                    otherParticipant = otherParticipant?.let(::toParticipantUiState),
                    participants = participantsExcludingSelf.map(::toParticipantUiState)
                        .toImmutableList(),
                )
            }
        }
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
