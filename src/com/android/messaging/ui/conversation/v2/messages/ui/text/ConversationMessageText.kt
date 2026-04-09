package com.android.messaging.ui.conversation.v2.messages.ui.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ConversationMessageText(
    text: String,
    style: TextStyle,
    onExternalUriClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary
    val linkStyle = remember(linkColor) {
        TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
            ),
        )
    }

    val textWithLinks by produceState(
        initialValue = AnnotatedString(text = text),
        text,
        linkStyle,
        onExternalUriClick,
        context.applicationContext,
    ) {
        val links = withContext(Dispatchers.IO) {
            extractConversationTextLinks(
                context = context.applicationContext,
                text = text,
            )
        }

        value = buildConversationLinkedAnnotatedString(
            text = text,
            links = links,
            linkStyle = linkStyle,
            onExternalUriClick = onExternalUriClick,
        )
    }

    Text(
        text = textWithLinks,
        style = style,
        modifier = modifier,
    )
}

private fun buildConversationLinkedAnnotatedString(
    text: String,
    links: List<ConversationTextLink>,
    linkStyle: TextLinkStyles,
    onExternalUriClick: (String) -> Unit,
): AnnotatedString {
    if (links.isEmpty()) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        var currentIndex = 0

        links.forEach { link ->
            if (link.start > currentIndex) {
                append(text.substring(currentIndex, link.start))
            }

            withLink(
                link = LinkAnnotation.Url(
                    url = link.url,
                    styles = linkStyle,
                    linkInteractionListener = { _ ->
                        onExternalUriClick(link.url)
                    },
                ),
            ) {
                append(text.substring(link.start, link.end))
            }

            currentIndex = link.end
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}
