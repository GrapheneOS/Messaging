package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import androidx.compose.runtime.Composable
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachmentKind

@Composable
internal fun ConversationInlineAttachmentRow(
    attachment: ConversationInlineAttachment,
    isIncoming: Boolean,
    isSelectionMode: Boolean,
    useStandaloneAudioAttachmentBackground: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onLongClick: () -> Unit = {},
) {
    val shouldUseEmbeddedAudioPlayer = attachment.kind == ConversationInlineAttachmentKind.AUDIO &&
        !attachment.contentUri.isNullOrBlank()

    when {
        shouldUseEmbeddedAudioPlayer -> {
            ConversationInlineAudioAttachmentRow(
                attachment = attachment,
                isIncoming = isIncoming,
                isSelectionMode = isSelectionMode,
                useStandaloneAudioAttachmentBackground = useStandaloneAudioAttachmentBackground,
                onLongClick = onLongClick,
            )
        }

        else -> {
            ConversationGenericInlineAttachmentRow(
                attachment = attachment,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
                onLongClick = onLongClick,
            )
        }
    }
}
