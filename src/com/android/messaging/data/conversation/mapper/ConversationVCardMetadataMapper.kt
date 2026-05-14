package com.android.messaging.data.conversation.mapper

import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.datamodel.data.VCardContactItemData
import com.android.messaging.datamodel.media.VCardResourceEntry
import javax.inject.Inject

internal interface ConversationVCardMetadataMapper {
    fun map(vCardContactItemData: VCardContactItemData): ConversationVCardAttachmentMetadata
}

internal class ConversationVCardMetadataMapperImpl @Inject constructor() :
    ConversationVCardMetadataMapper {

    override fun map(
        vCardContactItemData: VCardContactItemData,
    ): ConversationVCardAttachmentMetadata {
        val firstEntry = vCardContactItemData
            .vCardResource
            ?.vCards
            ?.singleOrNull()

        val isLocation = firstEntry
            ?.getKind()
            ?.equals(
                VCardResourceEntry.KIND_LOCATION,
                ignoreCase = true,
            ) == true

        return ConversationVCardAttachmentMetadata.Loaded(
            type = when {
                isLocation -> ConversationVCardAttachmentType.LOCATION
                else -> ConversationVCardAttachmentType.CONTACT
            },
            avatarUri = vCardContactItemData
                .avatarUri
                ?.toString()
                ?.takeIf { avatarUri -> avatarUri.isNotBlank() },
            displayName = vCardContactItemData
                .displayName
                ?.takeIf { title -> title.isNotBlank() },
            details = vCardContactItemData
                .details
                ?.takeIf { subtitle -> subtitle.isNotBlank() },
            locationAddress = firstEntry
                ?.displayAddress
                ?.takeIf { subtitle -> subtitle.isNotBlank() },
        )
    }
}
