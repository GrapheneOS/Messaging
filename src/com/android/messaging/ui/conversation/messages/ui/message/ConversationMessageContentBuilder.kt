package com.android.messaging.ui.conversation.messages.ui.message

import android.net.Uri
import com.android.messaging.R
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationMessageAttachment
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageContent
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.ui.attachment.buildConversationAttachmentSections
import kotlinx.collections.immutable.toImmutableList

internal fun buildConversationMessageContent(
    message: ConversationMessageUiModel,
    subjectText: String?,
): ConversationMessageContent {
    val attachments = message
        .parts
        .mapIndexedNotNull(::toConversationMessageAttachment)
        .toImmutableList()
    val attachmentSections = buildConversationAttachmentSections(
        attachments = attachments,
        vCardSubtitleTextResIdOverride = vCardSubtitleTextResIdOverride(message),
    )

    val bodyText = buildConversationMessageBodyText(
        message = message,
    )

    val isAttachmentOnly = subjectText.isNullOrBlank() &&
        bodyText.isNullOrBlank() &&
        attachments.isNotEmpty()

    return ConversationMessageContent(
        subjectText = subjectText,
        bodyText = bodyText,
        attachments = attachments,
        attachmentSections = attachmentSections,
        isAttachmentOnly = isAttachmentOnly,
    )
}

private fun vCardSubtitleTextResIdOverride(message: ConversationMessageUiModel): Int? {
    return when {
        message.canResendMessage -> R.string.message_status_send_failed
        else -> null
    }
}

private fun toConversationMessageAttachment(
    index: Int,
    part: ConversationMessagePartUiModel,
): ConversationMessageAttachment? {
    val attachmentPart = part as? ConversationMessagePartUiModel.Attachment ?: return null

    val key = buildConversationMessageAttachmentKey(
        index = index,
        contentType = attachmentPart.contentType,
        contentUri = attachmentPart.contentUri,
    )

    return when {
        attachmentPart.isSupportedAttachment() && attachmentPart.contentUri != null -> {
            ConversationMessageAttachment.Media(
                key = key,
                part = attachmentPart,
            )
        }

        else -> {
            ConversationMessageAttachment.Unsupported(
                key = key,
                part = attachmentPart,
            )
        }
    }
}

private fun buildConversationMessageAttachmentKey(
    index: Int,
    contentType: String,
    contentUri: Uri?,
): String {
    return buildString {
        append(index)
        append(':')
        append(contentType)
        append(':')
        append(contentUri ?: "missing")
    }
}

private fun buildConversationMessageBodyText(message: ConversationMessageUiModel): String? {
    message.text
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { bodyText ->
            return bodyText
        }

    return message.parts
        .asSequence()
        .filter { it.hasCaptionText }
        .mapNotNull { part ->
            part.text?.trim()?.takeIf { text -> text.isNotEmpty() }
        }
        .distinct()
        .joinToString(separator = "\n")
        .takeIf { text -> text.isNotEmpty() }
}

private fun ConversationMessagePartUiModel.Attachment.isSupportedAttachment(): Boolean {
    return when (this) {
        is ConversationMessagePartUiModel.Attachment.Audio,
        is ConversationMessagePartUiModel.Attachment.Image,
        is ConversationMessagePartUiModel.Attachment.VCard,
        is ConversationMessagePartUiModel.Attachment.Video,
        -> true

        is ConversationMessagePartUiModel.Attachment.File -> false
    }
}
