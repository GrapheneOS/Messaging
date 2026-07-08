package com.android.messaging.data.conversation.mapper

import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.data.vcard.mapper.VCardEntrySummarizer
import com.android.messaging.datamodel.media.CustomVCardEntry
import javax.inject.Inject

internal interface ConversationVCardMetadataMapper {
    fun map(entries: List<CustomVCardEntry>): ConversationVCardAttachmentMetadata
}

internal class ConversationVCardMetadataMapperImpl @Inject constructor(
    private val entrySummarizer: VCardEntrySummarizer,
) : ConversationVCardMetadataMapper {

    override fun map(entries: List<CustomVCardEntry>): ConversationVCardAttachmentMetadata {
        if (entries.isEmpty()) {
            return ConversationVCardAttachmentMetadata.Failed
        }

        val singleEntry = entries.singleOrNull()
        val isLocation = singleEntry != null && entrySummarizer.isLocation(singleEntry)

        return ConversationVCardAttachmentMetadata.Loaded(
            type = when {
                isLocation -> ConversationVCardAttachmentType.LOCATION
                else -> ConversationVCardAttachmentType.CONTACT
            },
            avatarPhoto = singleEntry?.let(entrySummarizer::avatarPhoto),
            entryCount = entries.size,
            singleDisplayName = singleEntry?.let(entrySummarizer::displayName),
            normalizedDestination = singleEntry?.let(entrySummarizer::normalizedDestination),
            locationAddress = singleEntry?.let(entrySummarizer::firstPostalAddress),
        )
    }
}
