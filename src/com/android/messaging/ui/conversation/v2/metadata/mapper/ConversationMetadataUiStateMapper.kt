package com.android.messaging.ui.conversation.v2.metadata.mapper

import com.android.messaging.data.conversation.model.metadata.ConversationMetadata
import com.android.messaging.sms.MmsSmsUtils
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import javax.inject.Inject

internal interface ConversationMetadataUiStateMapper {
    fun map(metadata: ConversationMetadata): ConversationMetadataUiState
}

internal class ConversationMetadataUiStateMapperImpl @Inject constructor() :
    ConversationMetadataUiStateMapper {

    override fun map(metadata: ConversationMetadata): ConversationMetadataUiState {
        val avatar = when {
            metadata.isGroupConversation -> ConversationMetadataUiState.Avatar.Group

            else -> {
                ConversationMetadataUiState.Avatar.Single(
                    photoUri = metadata.otherParticipantPhotoUri,
                )
            }
        }

        return ConversationMetadataUiState.Present(
            title = metadata.conversationName,
            selfParticipantId = metadata.selfParticipantId,
            avatar = avatar,
            participantCount = metadata.participantCount,
            otherParticipantDisplayDestination = metadata.otherParticipantDisplayDestination,
            otherParticipantPhoneNumber = metadata
                .otherParticipantNormalizedDestination
                ?.takeIf(MmsSmsUtils::isPhoneNumber),
            otherParticipantContactLookupKey = metadata.otherParticipantContactLookupKey,
            isArchived = metadata.isArchived,
            composerAvailability = metadata.composerAvailability,
        )
    }
}
