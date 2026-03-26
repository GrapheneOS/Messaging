package com.android.messaging.ui.conversation.v2.metadata.mapper

import com.android.messaging.data.conversation.repository.ConversationMetadata
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import javax.inject.Inject

internal interface ConversationMetadataUiStateMapper {
    fun map(metadata: ConversationMetadata): ConversationMetadataUiState
}

internal class ConversationMetadataUiStateMapperImpl @Inject constructor() : ConversationMetadataUiStateMapper {

    override fun map(metadata: ConversationMetadata): ConversationMetadataUiState {
        return ConversationMetadataUiState.Present(
            title = metadata.conversationName,
            selfParticipantId = metadata.selfParticipantId,
            isGroupConversation = metadata.isGroupConversation,
            participantCount = metadata.participantCount,
        )
    }
}
