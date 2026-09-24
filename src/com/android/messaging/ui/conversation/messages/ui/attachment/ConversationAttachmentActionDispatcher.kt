package com.android.messaging.ui.conversation.messages.ui.attachment

import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentOpenAction
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationMessageAttachment

internal fun dispatchConversationAttachmentOpenAction(
    action: ConversationAttachmentOpenAction,
    onAttachmentClick: OnConversationAttachmentClick,
    onExternalUriClick: (String) -> Unit,
) {
    when (action) {
        is ConversationAttachmentOpenAction.OpenContent -> {
            onAttachmentClick(
                action.contentType,
                action.contentUri,
                action.partId,
            )
        }

        is ConversationAttachmentOpenAction.OpenExternal -> {
            onExternalUriClick(action.uri)
        }
    }
}

/**
 * Opens the attachment, or null when there's nothing to open, or when the message takes the tap
 * instead, as it then passes no [onAttachmentClick].
 */
internal fun ConversationAttachmentOpenAction?.toConversationAttachmentClickOrNull(
    onAttachmentClick: OnConversationAttachmentClick?,
    onExternalUriClick: (String) -> Unit,
): (() -> Unit)? {
    if (this == null || onAttachmentClick == null) {
        return null
    }

    return {
        dispatchConversationAttachmentOpenAction(
            action = this,
            onAttachmentClick = onAttachmentClick,
            onExternalUriClick = onExternalUriClick,
        )
    }
}

/**
 * Tap opens the attachment, long press selects its message. An attachment without a tap takes no
 * gesture at all: a screen reader reads it with its message, and touches reach the message.
 */
@Composable
internal fun Modifier.conversationAttachmentClickable(
    onClick: (() -> Unit)?,
    onLongClick: () -> Unit,
): Modifier {
    return when (onClick) {
        null -> this

        else -> {
            combinedClickable(
                onClickLabel = stringResource(id = R.string.conversation_attachment_open),
                onLongClickLabel = stringResource(id = R.string.conversation_message_select),
                onClick = onClick,
                onLongClick = onLongClick,
            )
        }
    }
}

internal fun ConversationMessageAttachment.toConversationAttachmentOpenActionOrNull():
    ConversationAttachmentOpenAction? {
    return when (this) {
        is ConversationMessageAttachment.Media -> {
            ConversationAttachmentOpenAction.OpenContent(
                contentType = part.contentType,
                contentUri = part.contentUri.toString(),
                partId = part.partId,
            )
        }

        is ConversationMessageAttachment.Unsupported -> {
            part.contentUri?.let { contentUri ->
                ConversationAttachmentOpenAction.OpenContent(
                    contentType = part.contentType,
                    contentUri = contentUri.toString(),
                    partId = part.partId,
                )
            }
        }

        is ConversationMessageAttachment.YouTubePreview -> {
            ConversationAttachmentOpenAction.OpenExternal(
                uri = sourceUrl,
            )
        }
    }
}
