package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachmentKind

@Composable
internal fun ConversationInlineAttachmentRow(
    attachment: ConversationInlineAttachment,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onLongClick: () -> Unit = {},
) {
    val title = attachment.titleText
        ?: attachment.titleTextResId?.let { stringResource(it) }.orEmpty()

    val subtitle = attachment.subtitleTextResId?.let { stringResource(it) }

    val onClick = attachment.openAction?.let { action ->
        {
            dispatchConversationAttachmentOpenAction(
                action = action,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
            )
        }
    }

    val shape = RoundedCornerShape(size = MESSAGE_ATTACHMENT_CORNER_RADIUS)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = shape)
            .combinedClickable(
                enabled = true,
                onClick = {
                    onClick?.invoke()
                },
                onLongClick = onLongClick,
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = shape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(size = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                ConversationInlineAttachmentIcon(
                    kind = attachment.kind,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(space = 2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                subtitle?.let {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationInlineAttachmentIcon(
    kind: ConversationInlineAttachmentKind,
) {
    when (kind) {
        ConversationInlineAttachmentKind.AUDIO -> {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    id = R.string.audio_play_content_description,
                ),
            )
        }

        ConversationInlineAttachmentKind.FILE -> {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
            )
        }

        ConversationInlineAttachmentKind.VCARD -> {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
            )
        }
    }
}
