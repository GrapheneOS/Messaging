package com.android.messaging.ui.conversation.v2.messages.ui.text

import android.content.Context
import android.net.Uri
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextLinks
import android.webkit.URLUtil
import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationTextLink(
    val start: Int,
    val end: Int,
    val url: String,
)

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
                )
            }
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
        TextClassifier.TYPE_URL -> URLUtil.guessUrl(rawLinkText)
        else -> null
    }
}

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
