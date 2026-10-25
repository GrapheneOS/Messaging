package com.android.messaging.ui.conversation.messages.ui.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLink
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLinkKind

private const val LINK_OCCURRENCE_TAG = "conversation-link-occurrence"

internal fun buildConversationLinkedAnnotatedString(
    text: String,
    links: List<ConversationTextLink>,
    linkStyle: TextLinkStyles,
    onExternalUriClick: (String) -> Unit,
): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        links.forEach { link ->
            addLink(
                url = LinkAnnotation.Url(
                    url = link.url,
                    styles = linkStyle,
                    linkInteractionListener = { onExternalUriClick(link.url) },
                ),
                start = link.start,
                end = link.end,
            )
            addStringAnnotation(
                tag = LINK_OCCURRENCE_TAG,
                annotation = conversationLinkOccurrenceId(link = link),
                start = link.start,
                end = link.end,
            )
        }
    }
}

internal fun selectedConversationTextLink(
    selectedTexts: List<AnnotatedString>,
    links: List<ConversationTextLink>,
): ConversationTextLink? {
    val occurrences = selectedTexts.flatMap { text ->
        text.getStringAnnotations(tag = LINK_OCCURRENCE_TAG, start = 0, end = text.length)
            .filter { it.start < it.end }
            .map { it.item }
    }.toSet()
    val occurrence = occurrences.singleOrNull()
    return links.singleOrNull { conversationLinkOccurrenceId(link = it) == occurrence }
}

internal fun ConversationTextLink.clipboardText(): String {
    return when (kind) {
        ConversationTextLinkKind.Web -> url
        else -> originalText
    }
}

private fun conversationLinkOccurrenceId(link: ConversationTextLink): String {
    return "${link.start}:${link.end}"
}
