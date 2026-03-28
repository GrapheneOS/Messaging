package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.CONVERSATION_COMPOSE_BAR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_TEXT_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.v2.conversationShape
import com.android.messaging.ui.core.AppTheme

private val CONVERSATION_COMPOSE_BAR_FIELD_CORNER_RADIUS = 28.dp
private val CONVERSATION_COMPOSE_BAR_FIELD_SHAPE =
    RoundedCornerShape(size = CONVERSATION_COMPOSE_BAR_FIELD_CORNER_RADIUS)
private val CONVERSATION_COMPOSE_BAR_SINGLE_LINE_HEIGHT = 56.dp
private val CONVERSATION_COMPOSE_BAR_SEND_BUTTON_SIZE = CONVERSATION_COMPOSE_BAR_SINGLE_LINE_HEIGHT
private val CONVERSATION_COMPOSE_BAR_SEND_BUTTON_SPACING = 8.dp
private val CONVERSATION_COMPOSE_BAR_TEXT_FIELD_PADDING_HORIZONTAL = 12.dp
private val CONVERSATION_COMPOSE_BAR_TEXT_FIELD_PADDING_VERTICAL = 8.dp
private val CONVERSATION_COMPOSE_BAR_PREVIEW_PADDING_VERTICAL = 24.dp

@Composable
internal fun ConversationComposeBar(
    modifier: Modifier = Modifier,
    messageText: String,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
    onAttachmentClick: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    val presentation = rememberConversationComposeBarPresentation()

    ConversationComposeBarContainer(
        modifier = modifier,
    ) {
        ConversationComposeTextField(
            messageText = messageText,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isSendActionEnabled = isSendActionEnabled,
            presentation = presentation,
            onAttachmentClick = onAttachmentClick,
            onMessageTextChange = onMessageTextChange,
            onSendClick = onSendClick,
        )
    }
}

@Composable
private fun rememberConversationComposeBarPresentation(): ConversationComposeBarPresentation {
    val fieldColors = conversationComposeBarTextFieldColors()

    return remember(fieldColors) {
        ConversationComposeBarPresentation(
            fieldShape = CONVERSATION_COMPOSE_BAR_FIELD_SHAPE,
            fieldColors = fieldColors,
        )
    }
}

@Composable
private fun conversationComposeBarTextFieldColors(): TextFieldColors {
    return TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ConversationComposeBarContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .testTag(CONVERSATION_COMPOSE_BAR_TEST_TAG),
    ) {
        content()
    }
}

@Composable
private fun ConversationComposeTextField(
    messageText: String,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
    presentation: ConversationComposeBarPresentation,
    onAttachmentClick: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = CONVERSATION_COMPOSE_BAR_TEXT_FIELD_PADDING_HORIZONTAL,
                vertical = CONVERSATION_COMPOSE_BAR_TEXT_FIELD_PADDING_VERTICAL,
            ),
        horizontalArrangement = Arrangement.spacedBy(
            space = CONVERSATION_COMPOSE_BAR_SEND_BUTTON_SPACING
        ),
        verticalAlignment = Alignment.Bottom,
    ) {
        TextField(
            modifier = Modifier
                .weight(weight = 1f)
                .testTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
                .heightIn(min = CONVERSATION_COMPOSE_BAR_SINGLE_LINE_HEIGHT),
            value = messageText,
            onValueChange = onMessageTextChange,
            enabled = isMessageFieldEnabled,
            shape = presentation.fieldShape,
            colors = presentation.fieldColors,
            placeholder = {
                ConversationComposePlaceholder()
            },
            trailingIcon = {
                ConversationComposeImageAction(
                    enabled = isAttachmentActionEnabled,
                    onClick = onAttachmentClick,
                )
            },
            minLines = 1,
            maxLines = 4,
        )

        ConversationComposeSendAction(
            enabled = isSendActionEnabled,
            onClick = onSendClick,
        )
    }
}

@Composable
private fun ConversationComposePlaceholder() {
    Text(
        text = stringResource(id = R.string.compose_message_view_hint_text),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun ConversationComposeImageAction(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    IconButton(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        enabled = enabled,
    ) {
        Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = null,
        )
    }
}

@Composable
private fun ConversationComposeSendAction(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    FilledIconButton(
        modifier = Modifier
            .testTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .semantics {
                conversationShape = CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
            }
            .size(size = CONVERSATION_COMPOSE_BAR_SEND_BUTTON_SIZE),
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        enabled = enabled,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = stringResource(id = R.string.sendButtonContentDescription),
        )
    }
}

private data class ConversationComposeBarPresentation(
    val fieldShape: RoundedCornerShape,
    val fieldColors: TextFieldColors,
)

@Composable
private fun ConversationComposeBarPreviewContainer(
    content: @Composable () -> Unit,
) {
    AppTheme {
        Box(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .padding(vertical = CONVERSATION_COMPOSE_BAR_PREVIEW_PADDING_VERTICAL),
        ) {
            content()
        }
    }
}
