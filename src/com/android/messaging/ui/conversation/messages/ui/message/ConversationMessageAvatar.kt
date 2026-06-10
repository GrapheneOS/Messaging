package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.common.components.ParticipantAvatar
import com.android.messaging.ui.common.components.participantAvatarLabel
import com.android.messaging.ui.common.components.participantColorSeed
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.preview.previewIncomingMessage
import com.android.messaging.ui.core.MessagingPreviewColumn

internal val CONVERSATION_MESSAGE_AVATAR_SIZE = 32.dp
internal val CONVERSATION_MESSAGE_AVATAR_GAP = 8.dp
internal val CONVERSATION_MESSAGE_AVATAR_GUTTER_WIDTH =
    CONVERSATION_MESSAGE_AVATAR_SIZE + CONVERSATION_MESSAGE_AVATAR_GAP

private val CONVERSATION_MESSAGE_AVATAR_FALLBACK_SIZE = 18.dp

@Composable
internal fun ConversationMessageAvatar(
    modifier: Modifier = Modifier,
    message: ConversationMessageUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    ParticipantAvatar(
        avatarUri = message.senderAvatarUri?.toString(),
        size = CONVERSATION_MESSAGE_AVATAR_SIZE,
        fallbackLabel = participantAvatarLabel(source = message.senderDisplayName),
        modifier = modifier
            .clip(shape = CircleShape)
            .combinedClickable(
                enabled = true,
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
        colorSeedCode = participantColorSeed(
            normalizedDestination = message.senderNormalizedDestination,
        ),
        fallbackSize = CONVERSATION_MESSAGE_AVATAR_FALLBACK_SIZE,
        fallbackIcon = Icons.Rounded.Person,
    )
}

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
