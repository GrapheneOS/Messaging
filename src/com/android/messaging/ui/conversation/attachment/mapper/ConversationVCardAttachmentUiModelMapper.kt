package com.android.messaging.ui.conversation.attachment.mapper

import com.android.messaging.R
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import javax.inject.Inject

internal interface ConversationVCardAttachmentUiModelMapper {
    fun map(
        metadata: ConversationVCardAttachmentMetadata?,
    ): ConversationVCardAttachmentUiModel
}

internal class ConversationVCardAttachmentUiModelMapperImpl @Inject constructor() :
    ConversationVCardAttachmentUiModelMapper {

    override fun map(
        metadata: ConversationVCardAttachmentMetadata?,
    ): ConversationVCardAttachmentUiModel {
        return mapConversationVCardAttachmentUiModel(
            metadata = metadata,
            defaultTitleTextResId = R.string.notification_vcard,
            defaultSubtitleTextResId = R.string.vcard_tap_hint,
            failedSubtitleTextResId = R.string.failed_loading_vcard,
            loadingSubtitleTextResId = R.string.loading_vcard,
            locationTitleTextResId = R.string.notification_location,
        )
    }

    private fun mapConversationVCardAttachmentUiModel(
        metadata: ConversationVCardAttachmentMetadata?,
        defaultTitleTextResId: Int?,
        defaultSubtitleTextResId: Int?,
        failedSubtitleTextResId: Int,
        loadingSubtitleTextResId: Int,
        locationTitleTextResId: Int,
    ): ConversationVCardAttachmentUiModel {
        return when (metadata) {
            ConversationVCardAttachmentMetadata.Failed -> {
                createConversationContactUiModel(
                    avatarUri = null,
                    titleText = null,
                    titleTextResId = defaultTitleTextResId,
                    subtitleText = null,
                    subtitleTextResId = failedSubtitleTextResId,
                )
            }

            ConversationVCardAttachmentMetadata.Loading -> {
                createConversationContactUiModel(
                    avatarUri = null,
                    titleText = null,
                    titleTextResId = defaultTitleTextResId,
                    subtitleText = null,
                    subtitleTextResId = loadingSubtitleTextResId,
                )
            }

            ConversationVCardAttachmentMetadata.Missing,
            null,
            -> {
                createConversationContactUiModel(
                    avatarUri = null,
                    titleText = null,
                    titleTextResId = defaultTitleTextResId,
                    subtitleText = null,
                    subtitleTextResId = defaultSubtitleTextResId,
                )
            }

            is ConversationVCardAttachmentMetadata.Loaded -> {
                mapLoadedConversationVCardAttachmentUiModel(
                    metadata = metadata,
                    defaultTitleTextResId = defaultTitleTextResId,
                    defaultSubtitleTextResId = defaultSubtitleTextResId,
                    locationTitleTextResId = locationTitleTextResId,
                )
            }
        }
    }

    private fun mapLoadedConversationVCardAttachmentUiModel(
        metadata: ConversationVCardAttachmentMetadata.Loaded,
        defaultTitleTextResId: Int?,
        defaultSubtitleTextResId: Int?,
        locationTitleTextResId: Int,
    ): ConversationVCardAttachmentUiModel {
        return when (metadata.type) {
            ConversationVCardAttachmentType.CONTACT -> {
                createConversationContactUiModel(
                    avatarUri = metadata.avatarUri,
                    titleText = metadata.displayName,
                    titleTextResId = if (metadata.displayName == null) {
                        defaultTitleTextResId
                    } else {
                        null
                    },
                    subtitleText = metadata.details,
                    subtitleTextResId = if (metadata.details == null) {
                        defaultSubtitleTextResId
                    } else {
                        null
                    },
                )
            }

            ConversationVCardAttachmentType.LOCATION -> {
                ConversationVCardAttachmentUiModel(
                    type = ConversationVCardAttachmentType.LOCATION,
                    avatarUri = metadata.avatarUri,
                    titleText = metadata.displayName,
                    titleTextResId = if (metadata.displayName == null) {
                        locationTitleTextResId
                    } else {
                        null
                    },
                    subtitleText = metadata.locationAddress ?: metadata.details,
                    subtitleTextResId = null,
                )
            }
        }
    }

    private fun createConversationContactUiModel(
        avatarUri: String?,
        titleText: String?,
        titleTextResId: Int?,
        subtitleText: String?,
        subtitleTextResId: Int?,
    ): ConversationVCardAttachmentUiModel {
        return ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.CONTACT,
            avatarUri = avatarUri,
            titleText = titleText,
            titleTextResId = titleTextResId,
            subtitleText = subtitleText,
            subtitleTextResId = subtitleTextResId,
        )
    }
}
