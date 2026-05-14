package com.android.messaging.ui.conversation.messages.ui.attachment

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.attachment.ui.ConversationVCardAttachmentCardContent
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment

@Composable
internal fun ConversationVCardInlineAttachmentRow(
    attachment: ConversationInlineAttachment.VCard,
    isSelectionMode: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onLongClick: () -> Unit,
) {
    val onClick = attachment.openAction?.let { action ->
        {
            dispatchConversationAttachmentOpenAction(
                action = action,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
            )
        }
    }

    ConversationVCardInlineAttachmentRowContent(
        attachment = attachment,
        isSelectionMode = isSelectionMode,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
internal fun ConversationVCardInlineAttachmentRowContent(
    attachment: ConversationInlineAttachment.VCard,
    isSelectionMode: Boolean,
    onClick: (() -> Unit)?,
    onLongClick: () -> Unit,
) {
    val modifier = when {
        isSelectionMode -> Modifier
        else -> {
            Modifier.combinedClickable(
                onClick = {
                    onClick?.invoke()
                },
                onLongClick = onLongClick,
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(other = modifier),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(size = MESSAGE_ATTACHMENT_CORNER_RADIUS),
    ) {
        ConversationVCardAttachmentCardContent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            type = attachment.type,
            avatarUri = attachment.avatarUri,
            titleText = attachment.titleText,
            titleTextResId = attachment.titleTextResId,
            subtitleText = attachment.subtitleText,
            subtitleTextResId = attachment.subtitleTextResId,
        )
    }
}
