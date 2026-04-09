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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_COMPOSE_BAR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_TEXT_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.v2.conversationShape
import com.android.messaging.ui.core.AppTheme

@Composable
internal fun ConversationComposeBar(
    modifier: Modifier = Modifier,
    messageText: String,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
    messageFieldFocusRequester: FocusRequester? = null,
    onAttachmentClick: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    val presentation = rememberConversationComposeBarPresentation()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .testTag(CONVERSATION_COMPOSE_BAR_TEST_TAG),
    ) {
        ConversationComposeTextField(
            messageText = messageText,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isSendActionEnabled = isSendActionEnabled,
            messageFieldFocusRequester = messageFieldFocusRequester,
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
            fieldShape = RoundedCornerShape(size = 28.dp),
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
private fun ConversationComposeTextField(
    messageText: String,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
    messageFieldFocusRequester: FocusRequester?,
    presentation: ConversationComposeBarPresentation,
    onAttachmentClick: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
        ),
        verticalAlignment = Alignment.Bottom,
    ) {
        TextField(
            modifier = Modifier
                .weight(weight = 1f)
                .then(
                    when (messageFieldFocusRequester) {
                        null -> Modifier
                        else -> Modifier.focusRequester(messageFieldFocusRequester)
                    },
                )
                .testTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
                .heightIn(min = 56.dp),
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
                    modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG),
                    enabled = isAttachmentActionEnabled,
                    onClick = onAttachmentClick,
                )
            },
            minLines = 1,
            maxLines = 4,
        )

        ConversationComposeSendAction(
            modifier = Modifier
                .testTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
                .semantics {
                    conversationShape = CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
                },
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
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    IconButton(
        modifier = modifier,
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
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ConversationSendActionButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    )
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
                .padding(vertical = 24.dp),
        ) {
            content()
        }
    }
}
