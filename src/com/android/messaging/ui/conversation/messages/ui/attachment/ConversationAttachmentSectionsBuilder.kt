package com.android.messaging.ui.conversation.messages.ui.attachment

import com.android.messaging.R
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentItem
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentOpenAction
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentSections
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationMessageAttachment
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.util.ContentType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun buildConversationAttachmentSections(
    attachments: ImmutableList<ConversationMessageAttachment>,
    vCardSubtitleTextResIdOverride: Int? = null,
): ConversationAttachmentSections {
    val galleryVisualAttachments = attachments
        .asSequence()
        .filter(::isGalleryVisualAttachment)
        .toImmutableList()

    val trailingItems = attachments
        .asSequence()
        .filterNot(::isGalleryVisualAttachment)
        .mapNotNull { attachment ->
            toConversationAttachmentItem(
                attachment = attachment,
                vCardSubtitleTextResIdOverride = vCardSubtitleTextResIdOverride,
            )
        }
        .toImmutableList()

    return ConversationAttachmentSections(
        galleryVisualAttachments = galleryVisualAttachments,
        trailingItems = trailingItems,
    )
}

private fun isGalleryVisualAttachment(
    attachment: ConversationMessageAttachment,
): Boolean {
    return when (attachment) {
        is ConversationMessageAttachment.Media -> {
            attachment.part is ConversationMessagePartUiModel.Attachment.Image
        }

        is ConversationMessageAttachment.YouTubePreview -> true
        is ConversationMessageAttachment.Unsupported -> false
    }
}

private fun isStandaloneVisualAttachment(
    attachment: ConversationMessageAttachment,
): Boolean {
    return when (attachment) {
        is ConversationMessageAttachment.Media ->
            attachment.part is ConversationMessagePartUiModel.Attachment.Video

        is ConversationMessageAttachment.Unsupported,
        is ConversationMessageAttachment.YouTubePreview,
        -> false
    }
}

private fun toConversationAttachmentItem(
    attachment: ConversationMessageAttachment,
    vCardSubtitleTextResIdOverride: Int?,
): ConversationAttachmentItem? {
    return when {
        isStandaloneVisualAttachment(attachment = attachment) -> {
            ConversationAttachmentItem.StandaloneVisual(
                key = attachment.key,
                attachment = attachment,
            )
        }

        isInlineAttachment(attachment = attachment) -> {
            toInlineAttachment(
                attachment = attachment,
                vCardSubtitleTextResIdOverride = vCardSubtitleTextResIdOverride,
            )?.let { inlineAttachment ->
                ConversationAttachmentItem.Inline(
                    key = inlineAttachment.key,
                    attachment = inlineAttachment,
                )
            }
        }

        else -> null
    }
}

private fun isInlineAttachment(
    attachment: ConversationMessageAttachment,
): Boolean {
    return when (attachment) {
        is ConversationMessageAttachment.Media,
        is ConversationMessageAttachment.Unsupported,
        -> true

        else -> false
    }
}

private fun toInlineAttachment(
    attachment: ConversationMessageAttachment,
    vCardSubtitleTextResIdOverride: Int?,
): ConversationInlineAttachment? {
    return when (attachment) {
        is ConversationMessageAttachment.Media -> {
            toMediaInlineAttachment(
                attachment = attachment,
                vCardSubtitleTextResIdOverride = vCardSubtitleTextResIdOverride,
            )
        }

        is ConversationMessageAttachment.Unsupported -> {
            createFileInlineAttachment(
                key = attachment.key,
                part = attachment.part,
                openAction = attachment.toConversationAttachmentOpenActionOrNull(),
            )
        }

        is ConversationMessageAttachment.YouTubePreview -> null
    }
}

private fun toMediaInlineAttachment(
    attachment: ConversationMessageAttachment.Media,
    vCardSubtitleTextResIdOverride: Int?,
): ConversationInlineAttachment? {
    return when (val part = attachment.part) {
        is ConversationMessagePartUiModel.Attachment.Audio -> {
            createAudioInlineAttachment(
                key = attachment.key,
                contentUri = part.contentUri.toString(),
                openAction = attachment.toConversationAttachmentOpenActionOrNull(),
                durationMillis = part.durationMillis,
            )
        }

        is ConversationMessagePartUiModel.Attachment.VCard -> {
            createVCardInlineAttachment(
                key = attachment.key,
                contentUri = part.contentUri.toString(),
                openAction = attachment.toConversationAttachmentOpenActionOrNull(),
                vCardUiModel = part.vCardUiModel,
                subtitleTextResIdOverride = vCardSubtitleTextResIdOverride,
            )
        }

        is ConversationMessagePartUiModel.Attachment.Image,
        is ConversationMessagePartUiModel.Attachment.Video,
        -> null

        is ConversationMessagePartUiModel.Attachment.File -> {
            createFileInlineAttachment(
                key = attachment.key,
                part = part,
                openAction = attachment.toConversationAttachmentOpenActionOrNull(),
            )
        }
    }
}

private fun createAudioInlineAttachment(
    key: String,
    contentUri: String,
    openAction: ConversationAttachmentOpenAction?,
    durationMillis: Long,
): ConversationInlineAttachment {
    return ConversationInlineAttachment.Audio(
        key = key,
        contentUri = contentUri,
        openAction = openAction,
        titleText = null,
        titleTextResId = R.string.audio_attachment_content_description,
        durationMillis = durationMillis,
    )
}

private fun createVCardInlineAttachment(
    key: String,
    contentUri: String,
    openAction: ConversationAttachmentOpenAction?,
    vCardUiModel: ConversationVCardAttachmentUiModel,
    subtitleTextResIdOverride: Int?,
): ConversationInlineAttachment {
    return ConversationInlineAttachment.VCard(
        key = key,
        contentUri = contentUri,
        openAction = openAction,
        type = vCardUiModel.type,
        avatarPhoto = vCardUiModel.avatarPhoto,
        normalizedDestination = vCardUiModel.normalizedDestination,
        titleText = vCardUiModel.titleText,
        titleTextResId = vCardUiModel.titleTextResId,
        subtitleText = when {
            subtitleTextResIdOverride == null -> vCardUiModel.subtitleText
            else -> null
        },
        subtitleTextResId = subtitleTextResIdOverride ?: vCardUiModel.subtitleTextResId,
    )
}

private fun createFileInlineAttachment(
    key: String,
    part: ConversationMessagePartUiModel.Attachment,
    openAction: ConversationAttachmentOpenAction?,
): ConversationInlineAttachment {
    val isPdf = ContentType.isPdfType(part.contentType)

    return ConversationInlineAttachment.File(
        key = key,
        openAction = openAction,
        subtitleTextResId = if (isPdf) R.string.notification_file else null,
        titleText = if (isPdf) null else part.contentType.ifBlank { null },
        titleTextResId = if (isPdf) R.string.pdf_attachment else R.string.notification_file,
    )
}
