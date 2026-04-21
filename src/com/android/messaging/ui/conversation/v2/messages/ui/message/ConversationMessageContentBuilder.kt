package com.android.messaging.ui.conversation.v2.messages.ui.message

import android.net.Uri
import android.util.Patterns
import android.webkit.URLUtil
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationMessageAttachment
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageContent
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.ui.attachment.buildConversationAttachmentSections
import com.android.messaging.util.YouTubeUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun buildConversationMessageContent(
    message: ConversationMessageUiModel,
    subjectText: String?,
): ConversationMessageContent {
    val attachments = buildConversationMessageAttachments(message = message)
    val attachmentSections = buildConversationAttachmentSections(attachments = attachments)

    val bodyText = buildConversationMessageBodyText(
        message = message,
        attachments = attachments,
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

private fun buildConversationMessageAttachments(
    message: ConversationMessageUiModel,
): ImmutableList<ConversationMessageAttachment> {
    val attachmentItems = message
        .parts
        .mapIndexedNotNull(::toConversationMessageAttachment)
        .toImmutableList()

    val hasImageAttachment = attachmentItems.any { attachment ->
        attachment is ConversationMessageAttachment.Media && attachment.part.isImageAttachment
    }

    if (hasImageAttachment) {
        return attachmentItems
    }

    return message.text
        ?.let(::findSingleYouTubePreview)
        ?.let { youtubePreview ->
            (attachmentItems + youtubePreview).toImmutableList()
        }
        ?: attachmentItems
}

private fun toConversationMessageAttachment(
    index: Int,
    part: ConversationMessagePartUiModel,
): ConversationMessageAttachment? {
    if (!part.isMediaAttachment) {
        return null
    }

    val key = buildConversationMessageAttachmentKey(
        index = index,
        contentType = part.contentType,
        contentUri = part.contentUri,
    )

    return when {
        part.isSupportedAttachment && part.hasRenderableContentUri -> {
            ConversationMessageAttachment.Media(
                key = key,
                part = part,
            )
        }

        else -> {
            ConversationMessageAttachment.Unsupported(
                key = key,
                part = part,
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

private fun buildConversationMessageBodyText(
    message: ConversationMessageUiModel,
    attachments: ImmutableList<ConversationMessageAttachment>,
): String? {
    message.text
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { bodyText ->
            return bodyText
        }

    val captionText = message.parts
        .asSequence()
        .filter { part -> part.hasCaptionText }
        .mapNotNull { part ->
            part.text?.trim()?.takeIf { text -> text.isNotEmpty() }
        }
        .distinct()
        .joinToString(separator = "\n")
        .takeIf { text -> text.isNotEmpty() }

    return when {
        captionText != null -> captionText
        else -> null
    }
}

private fun findSingleYouTubePreview(
    text: String,
): ConversationMessageAttachment.YouTubePreview? {
    return extractConversationWebUrls(text)
        .asSequence()
        .mapNotNull { sourceUrl ->
            val thumbnailUrl = YouTubeUtil
                .getYoutubePreviewImageLink(sourceUrl)
                ?: return@mapNotNull null

            ConversationMessageAttachment.YouTubePreview(
                key = "youtube:$sourceUrl",
                sourceUrl = sourceUrl,
                thumbnailUrl = thumbnailUrl,
            )
        }
        .take(2)
        .singleOrNull()
}

private fun extractConversationWebUrls(text: String): Set<String> {
    val webUrlMatcher = Patterns.WEB_URL.matcher(text)
    val urls = LinkedHashSet<String>()

    while (webUrlMatcher.find()) {
        webUrlMatcher
            .group()
            .takeIf { it.isNotBlank() }
            ?.let(URLUtil::guessUrl)
            ?.let(urls::add)
    }

    return urls
}
