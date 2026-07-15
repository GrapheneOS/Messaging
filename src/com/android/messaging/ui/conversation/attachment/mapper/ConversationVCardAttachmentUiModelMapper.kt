package com.android.messaging.ui.conversation.attachment.mapper

import android.content.Context
import com.android.messaging.R
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface ConversationVCardAttachmentUiModelMapper {
    fun map(
        metadata: ConversationVCardAttachmentMetadata?,
    ): ConversationVCardAttachmentUiModel
}

internal class ConversationVCardAttachmentUiModelMapperImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
) : ConversationVCardAttachmentUiModelMapper {

    override fun map(
        metadata: ConversationVCardAttachmentMetadata?,
    ): ConversationVCardAttachmentUiModel {
        return when (metadata) {
            ConversationVCardAttachmentMetadata.Failed -> {
                placeholderUiModel(R.string.failed_loading_vcard)
            }

            ConversationVCardAttachmentMetadata.Loading -> {
                placeholderUiModel(R.string.loading_vcard)
            }

            ConversationVCardAttachmentMetadata.Missing,
            null,
            -> {
                placeholderUiModel(R.string.vcard_tap_hint)
            }

            is ConversationVCardAttachmentMetadata.Loaded -> {
                loadedUiModel(metadata)
            }
        }
    }

    private fun loadedUiModel(
        metadata: ConversationVCardAttachmentMetadata.Loaded,
    ): ConversationVCardAttachmentUiModel {
        val titleText = titleText(metadata)
        val titleTextResId = when {
            titleText != null -> null

            metadata.type == ConversationVCardAttachmentType.LOCATION -> {
                R.string.notification_location
            }

            else -> R.string.notification_vcard
        }

        val locationAddress = when (metadata.type) {
            ConversationVCardAttachmentType.LOCATION -> metadata.locationAddress
            ConversationVCardAttachmentType.CONTACT -> null
        }

        return ConversationVCardAttachmentUiModel(
            type = metadata.type,
            avatarPhoto = metadata.avatarPhoto,
            normalizedDestination = metadata.normalizedDestination,
            titleText = titleText,
            titleTextResId = titleTextResId,
            subtitleText = locationAddress,
            subtitleTextResId = when (locationAddress) {
                null -> R.string.vcard_tap_hint
                else -> null
            },
        )
    }

    private fun titleText(
        metadata: ConversationVCardAttachmentMetadata.Loaded,
    ): String? {
        if (metadata.entryCount > 1) {
            return context.resources.getQuantityString(
                R.plurals.vcard_multiple_display_name,
                metadata.entryCount,
                metadata.entryCount,
            )
        }

        return metadata.singleDisplayName
    }

    private fun placeholderUiModel(
        subtitleTextResId: Int,
    ): ConversationVCardAttachmentUiModel {
        return ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.CONTACT,
            titleTextResId = R.string.notification_vcard,
            subtitleTextResId = subtitleTextResId,
        )
    }
}
