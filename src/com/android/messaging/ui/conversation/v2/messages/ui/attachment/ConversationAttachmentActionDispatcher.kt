package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationAttachmentOpenAction
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationMessageAttachment

internal fun dispatchConversationAttachmentOpenAction(
    action: ConversationAttachmentOpenAction,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
) {
    when (action) {
        is ConversationAttachmentOpenAction.OpenContent -> {
            onAttachmentClick(
                action.contentType,
                action.contentUri,
            )
        }

        is ConversationAttachmentOpenAction.OpenExternal -> {
            onExternalUriClick(action.uri)
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
            )
        }

        is ConversationMessageAttachment.Unsupported -> {
            part.contentUri?.let { contentUri ->
                ConversationAttachmentOpenAction.OpenContent(
                    contentType = part.contentType,
                    contentUri = contentUri.toString(),
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
