package com.android.messaging.data.conversation.repository

import com.android.messaging.data.conversation.mapper.ConversationVCardMetadataMapper
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.vcard.repository.VCardEntryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

internal interface ConversationVCardMetadataRepository {
    fun observeAttachmentMetadata(
        contentUri: String?,
    ): Flow<ConversationVCardAttachmentMetadata>
}

internal class ConversationVCardMetadataRepositoryImpl @Inject constructor(
    private val vCardEntryRepository: VCardEntryRepository,
    private val conversationVCardMetadataMapper: ConversationVCardMetadataMapper,
) : ConversationVCardMetadataRepository {

    override fun observeAttachmentMetadata(
        contentUri: String?,
    ): Flow<ConversationVCardAttachmentMetadata> {
        if (contentUri.isNullOrBlank()) {
            return flowOf(ConversationVCardAttachmentMetadata.Missing)
        }

        return flow {
            emit(ConversationVCardAttachmentMetadata.Loading)
            emit(conversationVCardMetadataMapper.map(vCardEntryRepository.getEntries(contentUri)))
        }
    }
}
