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

        val participants = participantsData
            .filter { !it.isSelf }
            .map(::toParticipantUiState)
            .toImmutableList()

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
                    participants = participants,
                )
            } else {
                ConversationSettingsUiState(
                    conversationId = conversationId,
                    conversationTitle = cursor.getString(
                        PeopleOptionsItemData.INDEX_CONVERSATION_NAME,
                    ).orEmpty(),
                    isArchived = cursor.getInt(
                        PeopleOptionsItemData.INDEX_ARCHIVE_STATUS,
                    ) == 1,
                    participants = participants,
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
