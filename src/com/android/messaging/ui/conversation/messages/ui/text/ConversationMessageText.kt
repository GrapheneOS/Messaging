package com.android.messaging.ui.conversation.messages.ui.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.core.MessagingPreviewColumn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREVIEW_PLAIN_TEXT = "Sounds good. I will be there in about 10 minutes."
private const val PREVIEW_MULTILINE_TEXT = "First line from the message\n" +
    "Second line with a little more detail\n" +
    "Third line after an intentional line break"
private const val PREVIEW_LONG_WRAPPING_TEXT = "This is a longer SMS body that should wrap " +
    "across multiple lines in a narrow message bubble. It gives the preview enough text to show " +
    "line height, paragraph density, and how links like https://example.com/details fit inside " +
    "the same text layout."
private const val PREVIEW_UNBROKEN_TEXT =
    "SupercalifragilisticexpialidociousPreviewTokenWithoutNaturalBreaks1234567890"
private const val PREVIEW_MIXED_LINK_TEXT = "Open https://example.com or call +31 6 2222 3333."
private const val PREVIEW_EMAIL_LINK_TEXT = "Send the receipt to preview@example.com before 5 PM."
private const val PREVIEW_ADDRESS_LINK_TEXT =
    "Meet at 1600 Amphitheatre Parkway, Mountain View, CA."
private const val PREVIEW_EMOJI_TEXT = "Dinner is ready \uD83C\uDF5C Bring drinks if you can."

internal val LocalConversationMessageLinkColor: ProvidableCompositionLocal<Color?> =
    compositionLocalOf {
        null
    }

@Composable
internal fun ConversationMessageText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle,
    onExternalUriClick: (String) -> Unit,
) {
    val applicationContext = LocalContext.current.applicationContext
    val state = LocalConversationTextSelection.current ?: remember(text) {
        ConversationTextSelectionState()
    }
    val currentOnExternalUriClick by rememberUpdatedState(newValue = onExternalUriClick)
    val coordinator = LocalConversationTextSelectionCoordinator.current
    val gesture = remember { ConversationTextLinkGesture() }
    val hapticFeedback = LocalHapticFeedback.current
    val linkStyle = rememberConversationTextLinkStyle()
    val linkClick = remember(state, coordinator, gesture) {
        { uri: String ->
            when {
                gesture.blocksClick -> Unit
                coordinator?.dismiss() == true -> Unit
                state.hasSelection -> state.selection.clear()
                else -> currentOnExternalUriClick(uri)
            }
        }
    }
    LaunchedEffect(applicationContext, text, state) {
        val links = withContext(Dispatchers.IO) {
            extractConversationTextLinks(context = applicationContext, text = text)
        }
        state.updateLinks(detectedLinks = links)
    }
    val textWithLinks = remember(text, state.links, linkStyle, linkClick) {
        buildConversationLinkedAnnotatedString(
            text = text,
            links = state.links,
            linkStyle = linkStyle,
            onExternalUriClick = linkClick,
        )
    }
    ConversationSelectableTextTouchGuard(modifier = modifier) {
        Text(
            text = textWithLinks,
            style = style,
            onTextLayout = { gesture.textLayoutResult = it },
            modifier = Modifier
                .conversationSelectableTextBounds()
                .onGloballyPositioned { gesture.coordinates = it }
                .selectConversationTextLinkOnLongPress(
                    gesture = gesture,
                    state = state,
                    hapticFeedback = hapticFeedback,
                ),
        )
    }
}

@Composable
private fun rememberConversationTextLinkStyle(): TextLinkStyles {
    val linkColor = LocalConversationMessageLinkColor.current
        ?: MaterialTheme.colorScheme.primary
    return remember(linkColor) {
        TextLinkStyles(
            style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageTextBasicPreview() {
    ConversationMessageTextPreviewColumn {
        ConversationMessageTextPreviewSample(
            text = PREVIEW_PLAIN_TEXT,
            style = MaterialTheme.typography.bodyLarge,
        )
        ConversationMessageTextPreviewSample(
            text = PREVIEW_MULTILINE_TEXT,
            style = MaterialTheme.typography.bodyLarge,
        )
        ConversationMessageTextPreviewSample(
            text = PREVIEW_EMOJI_TEXT,
            style = MaterialTheme.typography.bodyLarge,
        )
        ConversationMessageTextPreviewSample(
            text = "",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageTextWrappingPreview() {
    ConversationMessageTextPreviewColumn {
        ConversationMessageTextPreviewSample(
            text = PREVIEW_LONG_WRAPPING_TEXT,
            style = MaterialTheme.typography.bodyLarge,
        )
        ConversationMessageTextPreviewSample(
            modifier = Modifier.width(width = 180.dp),
            text = PREVIEW_LONG_WRAPPING_TEXT,
            style = MaterialTheme.typography.bodyLarge,
        )
        ConversationMessageTextPreviewSample(
            modifier = Modifier.width(width = 180.dp),
            text = PREVIEW_UNBROKEN_TEXT,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageTextLinksPreview() {
    ConversationMessageTextPreviewColumn {
        ConversationMessageTextPreviewSample(
            text = PREVIEW_MIXED_LINK_TEXT,
            style = MaterialTheme.typography.bodyLarge,
        )
        ConversationMessageTextPreviewSample(
            text = PREVIEW_EMAIL_LINK_TEXT,
            style = MaterialTheme.typography.bodyLarge,
        )
        ConversationMessageTextPreviewSample(
            text = PREVIEW_ADDRESS_LINK_TEXT,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageTextTypographyPreview() {
    ConversationMessageTextPreviewColumn {
        ConversationMessageTextPreviewSample(
            text = PREVIEW_PLAIN_TEXT,
            style = MaterialTheme.typography.bodyLarge,
        )
        ConversationMessageTextPreviewSample(
            text = PREVIEW_PLAIN_TEXT,
            style = MaterialTheme.typography.bodyMedium,
        )
        ConversationMessageTextPreviewSample(
            text = PREVIEW_PLAIN_TEXT,
            style = MaterialTheme.typography.labelMedium,
        )
        ConversationMessageTextPreviewSample(
            text = PREVIEW_PLAIN_TEXT,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageTextBubbleLinkColorPreview() {
    ConversationMessageTextPreviewColumn {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            CompositionLocalProvider(
                LocalConversationMessageLinkColor provides MaterialTheme.colorScheme.onPrimary,
            ) {
                ConversationMessageText(
                    modifier = Modifier.padding(all = 16.dp),
                    text = PREVIEW_MIXED_LINK_TEXT,
                    style = MaterialTheme.typography.bodyLarge,
                    onExternalUriClick = {},
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            ConversationMessageText(
                modifier = Modifier.padding(all = 16.dp),
                text = PREVIEW_LONG_WRAPPING_TEXT,
                style = MaterialTheme.typography.bodyLarge,
                onExternalUriClick = {},
            )
        }
    }
}

@Composable
private fun ConversationMessageTextPreviewColumn(content: @Composable () -> Unit) {
    MessagingPreviewColumn {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ConversationMessageTextPreviewSample(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        ConversationMessageText(
            modifier = Modifier.padding(all = 16.dp),
            text = text,
            style = style,
            onExternalUriClick = {},
        )
    }
}
