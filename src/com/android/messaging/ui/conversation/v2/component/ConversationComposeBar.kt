package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddCircleOutline
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.core.AppTheme

private val CONVERSATION_COMPOSE_BAR_FIELD_CORNER_RADIUS = 28.dp
private val CONVERSATION_COMPOSE_BAR_TEXT_FIELD_PADDING_HORIZONTAL = 12.dp
private val CONVERSATION_COMPOSE_BAR_TEXT_FIELD_PADDING_VERTICAL = 8.dp
private val CONVERSATION_COMPOSE_BAR_TRAILING_ACTION_SPACING = 2.dp
private val CONVERSATION_COMPOSE_BAR_PREVIEW_PADDING_VERTICAL = 24.dp

@Composable
internal fun ConversationComposeBar(
    modifier: Modifier = Modifier,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    val presentation = rememberConversationComposeBarPresentation()

    ConversationComposeBarContainer(
        modifier = modifier,
    ) {
        ConversationComposeTextField(
            value = value,
            enabled = enabled,
            presentation = presentation,
            onValueChange = onValueChange,
            onSendClick = onSendClick,
        )
    }
}

@Composable
private fun rememberConversationComposeBarPresentation(): ConversationComposeBarPresentation {
    val fieldShape = RoundedCornerShape(size = CONVERSATION_COMPOSE_BAR_FIELD_CORNER_RADIUS)
    val fieldColors = conversationComposeBarTextFieldColors()

    return remember(
        fieldShape,
        fieldColors,
    ) {
        ConversationComposeBarPresentation(
            fieldShape = fieldShape,
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Composable
private fun ConversationComposeTextField(
    value: String,
    enabled: Boolean,
    presentation: ConversationComposeBarPresentation,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = CONVERSATION_COMPOSE_BAR_TEXT_FIELD_PADDING_HORIZONTAL,
                vertical = CONVERSATION_COMPOSE_BAR_TEXT_FIELD_PADDING_VERTICAL,
            ),
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        shape = presentation.fieldShape,
        colors = presentation.fieldColors,
        placeholder = {
            ConversationComposePlaceholder()
        },
        leadingIcon = {
            ConversationComposeLeadingAction(
                enabled = enabled,
                onClick = onSendClick,
            )
        },
        trailingIcon = {
            ConversationComposeTrailingActions(
                enabled = enabled,
                onSendClick = onSendClick,
            )
        },
        maxLines = 4,
    )
}

@Composable
private fun ConversationComposePlaceholder() {
    Text(
        text = stringResource(id = R.string.compose_message_view_hint_text),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun ConversationComposeLeadingAction(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(
            imageVector = Icons.Rounded.AddCircleOutline,
            contentDescription = null,
        )
    }
}

@Composable
private fun ConversationComposeTrailingActions(
    enabled: Boolean,
    onSendClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = CONVERSATION_COMPOSE_BAR_TRAILING_ACTION_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConversationComposeImageAction(
            enabled = enabled,
            onClick = onSendClick,
        )

        ConversationComposeSendAction(
            enabled = enabled,
            onClick = onSendClick,
        )
    }
}

@Composable
private fun ConversationComposeImageAction(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
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
    IconButton(
        onClick = onClick,
        enabled = enabled,
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
