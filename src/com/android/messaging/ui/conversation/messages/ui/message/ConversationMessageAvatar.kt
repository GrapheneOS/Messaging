package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.messaging.sms.MmsSmsUtils
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.preview.previewIncomingMessage
import com.android.messaging.ui.core.MessagingPreviewColumn

internal val CONVERSATION_MESSAGE_AVATAR_SIZE = 32.dp
internal val CONVERSATION_MESSAGE_AVATAR_GAP = 8.dp
internal val CONVERSATION_MESSAGE_AVATAR_GUTTER_WIDTH =
    CONVERSATION_MESSAGE_AVATAR_SIZE + CONVERSATION_MESSAGE_AVATAR_GAP

@Composable
internal fun ConversationMessageAvatar(
    modifier: Modifier = Modifier,
    message: ConversationMessageUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    val label = remember(message.senderDisplayName) {
        conversationMessageAvatarLabel(displayName = message.senderDisplayName)
    }

    val fallbackColors = rememberConversationMessageAvatarColors(message = message)

    Box(
        modifier = modifier
            .size(size = CONVERSATION_MESSAGE_AVATAR_SIZE)
            .clip(shape = CircleShape)
            .combinedClickable(
                enabled = true,
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        ConversationMessageAvatarFallback(
            colors = fallbackColors,
            label = label,
        )

        message.senderAvatarUri?.let {
            AsyncImage(
                model = message.senderAvatarUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ConversationMessageAvatarFallback(
    modifier: Modifier = Modifier,
    colors: ConversationMessageAvatarColors,
    label: String?,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.container,
        contentColor = colors.content,
        shape = CircleShape,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            ConversationMessageAvatarFallbackContent(
                label = label,
            )
        }
    }
}

@Composable
private fun ConversationMessageAvatarFallbackContent(
    modifier: Modifier = Modifier,
    label: String?,
) {
    when (label) {
        null -> {
            Icon(
                modifier = modifier.size(size = 18.dp),
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
            )
        }

        else -> {
            Text(
                modifier = modifier,
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun rememberConversationMessageAvatarColors(
    message: ConversationMessageUiModel,
): ConversationMessageAvatarColors {
    val colorScheme = MaterialTheme.colorScheme

    val colorKey = remember(message) {
        conversationMessageAvatarColorKey(message = message)
    }

    return remember(colorScheme, colorKey) {
        val colorOptions = listOf(
            ConversationMessageAvatarColors(
                container = colorScheme.primaryContainer,
                content = colorScheme.onPrimaryContainer,
            ),
            ConversationMessageAvatarColors(
                container = colorScheme.secondaryContainer,
                content = colorScheme.onSecondaryContainer,
            ),
            ConversationMessageAvatarColors(
                container = colorScheme.tertiaryContainer,
                content = colorScheme.onTertiaryContainer,
            ),
            ConversationMessageAvatarColors(
                container = colorScheme.surfaceContainerHigh,
                content = colorScheme.onSurface,
            ),
            ConversationMessageAvatarColors(
                container = colorScheme.surfaceContainerHighest,
                content = colorScheme.onSurface,
            ),
        )

        val colorIndex = (colorKey.hashCode() and Int.MAX_VALUE) % colorOptions.size

        colorOptions[colorIndex]
    }
}

private fun conversationMessageAvatarColorKey(
    message: ConversationMessageUiModel,
): String {
    return message.senderParticipantId
        ?: message.senderContactLookupKey?.takeIf { it.isNotBlank() }
        ?: message.senderDisplayName?.takeIf { it.isNotBlank() }
        ?: message.conversationId
}

private fun conversationMessageAvatarLabel(
    displayName: String?,
): String? {
    val trimmedDisplayName = displayName?.trim()

    return when {
        trimmedDisplayName == null -> null
        trimmedDisplayName.isBlank() -> null
        MmsSmsUtils.isPhoneNumber(trimmedDisplayName) -> null
        else -> trimmedDisplayName.first().uppercaseChar().toString()
    }
}

private data class ConversationMessageAvatarColors(
    val container: Color,
    val content: Color,
)

@PreviewLightDark
@Composable
private fun ConversationMessageAvatarPreview() {
    MessagingPreviewColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            ConversationMessageAvatar(
                message = previewIncomingMessage(),
                onClick = {},
                onLongClick = {},
            )
            ConversationMessageAvatar(
                message = previewIncomingMessage(
                    messageId = "phone-avatar",
                    text = "Phone number sender",
                ).copy(senderDisplayName = "+31 6 2222 3333"),
                onClick = {},
                onLongClick = {},
            )
        }
    }
}
