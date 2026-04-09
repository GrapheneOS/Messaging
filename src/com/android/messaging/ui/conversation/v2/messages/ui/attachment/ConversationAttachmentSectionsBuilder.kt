package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationAttachmentItem
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationAttachmentOpenAction
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationAttachmentSections
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachmentKind
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationMessageAttachment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun buildConversationAttachmentSections(
    attachments: ImmutableList<ConversationMessageAttachment>,
): ConversationAttachmentSections {
    val galleryVisualAttachments = attachments
        .asSequence()
        .filter(::isGalleryVisualAttachment)
        .toImmutableList()

    val trailingItems = attachments
        .asSequence()
        .filterNot(::isGalleryVisualAttachment)
        .mapNotNull(::toConversationAttachmentItem)
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
        is ConversationMessageAttachment.Media -> attachment.part.isImageAttachment
        is ConversationMessageAttachment.YouTubePreview -> true
        is ConversationMessageAttachment.Unsupported -> false
    }
}

private fun isStandaloneVisualAttachment(
    attachment: ConversationMessageAttachment,
): Boolean {
    return when (attachment) {
        is ConversationMessageAttachment.Media -> attachment.part.isVideoAttachment

        is ConversationMessageAttachment.Unsupported,
        is ConversationMessageAttachment.YouTubePreview,
        -> false
    }
}

private fun toConversationAttachmentItem(
    attachment: ConversationMessageAttachment,
): ConversationAttachmentItem? {
    return when {
        isStandaloneVisualAttachment(attachment = attachment) -> {
            ConversationAttachmentItem.StandaloneVisual(
                key = attachment.key,
                attachment = attachment,
            )
        }

        isInlineAttachment(attachment = attachment) -> {
            toInlineAttachment(attachment = attachment)
                ?.let { inlineAttachment ->
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
): ConversationInlineAttachment? {
    return when (attachment) {
        is ConversationMessageAttachment.Media -> {
            toMediaInlineAttachment(
                attachment = attachment,
            )
        }

        is ConversationMessageAttachment.Unsupported -> {
            createFileInlineAttachment(
                key = attachment.key,
                titleText = attachment.part.contentType.ifBlank { null },
                openAction = attachment.toConversationAttachmentOpenActionOrNull(),
            )
        }

        is ConversationMessageAttachment.YouTubePreview -> null
    }
}

private fun toMediaInlineAttachment(
    attachment: ConversationMessageAttachment.Media,
): ConversationInlineAttachment? {
    return when {
        attachment.part.isAudioAttachment -> {
            createAudioInlineAttachment(
                key = attachment.key,
                openAction = attachment.toConversationAttachmentOpenActionOrNull(),
            )
        }

        attachment.part.isVCardAttachment -> {
            createVCardInlineAttachment(
                key = attachment.key,
                openAction = attachment.toConversationAttachmentOpenActionOrNull(),
            )
        }

        attachment.part.isImageAttachment || attachment.part.isVideoAttachment -> null

        else -> {
            createFileInlineAttachment(
                key = attachment.key,
                titleText = attachment.part.contentType.ifBlank { null },
                openAction = attachment.toConversationAttachmentOpenActionOrNull(),
            )
        }
    }
}

private fun createAudioInlineAttachment(
    key: String,
    openAction: ConversationAttachmentOpenAction?,
): ConversationInlineAttachment {
    return ConversationInlineAttachment(
        key = key,
        kind = ConversationInlineAttachmentKind.AUDIO,
        openAction = openAction,
        subtitleTextResId = null,
        titleText = null,
        titleTextResId = R.string.audio_attachment_content_description,
    )
}

private fun createVCardInlineAttachment(
    key: String,
    openAction: ConversationAttachmentOpenAction?,
): ConversationInlineAttachment {
    return ConversationInlineAttachment(
        key = key,
        kind = ConversationInlineAttachmentKind.VCARD,
        openAction = openAction,
        subtitleTextResId = R.string.vcard_tap_hint,
        titleText = null,
        titleTextResId = R.string.notification_vcard,
    )
}

private fun createFileInlineAttachment(
    key: String,
    titleText: String?,
    openAction: ConversationAttachmentOpenAction?,
): ConversationInlineAttachment {
    return ConversationInlineAttachment(
        key = key,
        kind = ConversationInlineAttachmentKind.FILE,
        openAction = openAction,
        subtitleTextResId = null,
        titleText = titleText,
        titleTextResId = R.string.notification_file,
    )
}
