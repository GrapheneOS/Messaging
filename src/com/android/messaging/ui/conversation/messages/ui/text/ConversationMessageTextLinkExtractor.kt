package com.android.messaging.ui.conversation.messages.ui.text

import android.content.Context
import android.net.Uri
import android.text.util.Linkify
import android.util.Patterns
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextLinks
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLink
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLinkKind

private data class ConversationLinkText(
    val start: Int,
    val end: Int,
    val entityType: String,
    val rawLinkText: String,
)

internal fun extractConversationTextLinks(
    context: Context,
    text: String,
): List<ConversationTextLink> {
    if (text.isBlank()) {
        return emptyList()
    }

    val request = TextLinks.Request.Builder(text)
        .setEntityConfig(CONVERSATION_TEXT_LINK_ENTITY_CONFIG)
        .build()

    val textClassifier = context
        .getSystemService(TextClassificationManager::class.java)
        ?.textClassifier
        ?: TextClassifier.NO_OP

    return textClassifier.generateLinks(request)
        .links
        .asSequence()
        .mapNotNull { textLink ->
            textLink.toConversationTextLink(text = text)
        }
        .sortedBy { it.start }
        .toList()
}

/**
 * Finds links where no text classifier runs, in previews and screenshots: URLs, emails and phone
 * numbers by the platform's patterns, with Linkify's five-digit minimum for phone numbers, but no
 * addresses. Where matches overlap, the first one wins.
 */
internal fun extractPreviewConversationTextLinks(text: String): List<ConversationTextLink> {
    return PREVIEW_LINK_PATTERNS
        .flatMap { (entityType, pattern) ->
            pattern.toRegex()
                .findAll(text)
                .filter { match ->
                    match.isPreviewLinkMatch(entityType = entityType, text = text)
                }
                .mapNotNull { match ->
                    resolveConversationTextLinkUrl(
                        entityType = entityType,
                        rawLinkText = match.value,
                    )?.let { url ->
                        ConversationTextLink(
                            start = match.range.first,
                            end = match.range.last + 1,
                            url = url,
                            kind = conversationTextLinkKind(entityType = entityType),
                            originalText = match.value,
                        )
                    }
                }
        }
        .sortedBy { it.start }
        .fold(emptyList()) { links, link ->
            when {
                links.isNotEmpty() && links.last().end > link.start -> links
                else -> links + link
            }
        }
}

private fun MatchResult.isPreviewLinkMatch(entityType: String, text: String): Boolean {
    return when (entityType) {
        TextClassifier.TYPE_PHONE -> {
            Linkify.sPhoneNumberMatchFilter.acceptMatch(text, range.first, range.last + 1)
        }

        else -> true
    }
}

private fun TextLinks.TextLink.toConversationTextLink(
    text: String,
): ConversationTextLink? {
    return toValidatedConversationLinkText(text = text)
        ?.let { linkText ->
            resolveConversationTextLinkUrl(
                entityType = linkText.entityType,
                rawLinkText = linkText.rawLinkText,
            )?.let { url ->
                ConversationTextLink(
                    start = linkText.start,
                    end = linkText.end,
                    url = url,
                    kind = conversationTextLinkKind(entityType = linkText.entityType),
                    originalText = linkText.rawLinkText,
                )
            }
        }
}

private fun conversationTextLinkKind(entityType: String): ConversationTextLinkKind {
    return when (entityType) {
        TextClassifier.TYPE_PHONE -> ConversationTextLinkKind.Phone
        TextClassifier.TYPE_EMAIL -> ConversationTextLinkKind.Email
        TextClassifier.TYPE_ADDRESS -> ConversationTextLinkKind.Address
        else -> ConversationTextLinkKind.Web
    }
}

private fun TextLinks.TextLink.toValidatedConversationLinkText(
    text: String,
): ConversationLinkText? {
    val isValidLinkText = start in 0..<end && end <= text.length && entityCount > 0

    return when {
        isValidLinkText -> {
            text
                .substring(startIndex = start, endIndex = end)
                .takeIf { it.isNotBlank() }
                ?.let { rawLinkText ->
                    ConversationLinkText(
                        start = start,
                        end = end,
                        entityType = getEntity(0),
                        rawLinkText = rawLinkText,
                    )
                }
        }

        else -> null
    }
}

private fun resolveConversationTextLinkUrl(
    entityType: String,
    rawLinkText: String,
): String? {
    return when (entityType) {
        TextClassifier.TYPE_ADDRESS -> "geo:0,0?q=${Uri.encode(rawLinkText)}"
        TextClassifier.TYPE_EMAIL -> "mailto:${Uri.encode(rawLinkText)}"
        TextClassifier.TYPE_PHONE -> "tel:${Uri.encode(rawLinkText)}"
        TextClassifier.TYPE_URL -> resolveConversationWebUrl(rawLinkText = rawLinkText)
        else -> null
    }
}

private fun resolveConversationWebUrl(rawLinkText: String): String {
    val matchingScheme = WEB_URL_SCHEMES.firstOrNull { scheme ->
        rawLinkText.startsWith(prefix = scheme, ignoreCase = true)
    }

    return when (matchingScheme) {
        null -> "$DEFAULT_WEB_URL_SCHEME$rawLinkText"
        else -> "$matchingScheme${rawLinkText.substring(startIndex = matchingScheme.length)}"
    }
}

private const val DEFAULT_WEB_URL_SCHEME = "http://"

private val WEB_URL_SCHEMES = setOf(
    DEFAULT_WEB_URL_SCHEME,
    "https://",
    "rtsp://",
    "ftp://",
)

private val CONVERSATION_TEXT_LINK_ENTITY_CONFIG = TextClassifier.EntityConfig.Builder()
    .setIncludedTypes(
        listOf(
            TextClassifier.TYPE_ADDRESS,
            TextClassifier.TYPE_EMAIL,
            TextClassifier.TYPE_PHONE,
            TextClassifier.TYPE_URL,
        ),
    )
    .includeTypesFromTextClassifier(false)
    .build()

private val PREVIEW_LINK_PATTERNS = listOf(
    TextClassifier.TYPE_URL to Patterns.WEB_URL,
    TextClassifier.TYPE_EMAIL to Patterns.EMAIL_ADDRESS,
    TextClassifier.TYPE_PHONE to Patterns.PHONE,
)
