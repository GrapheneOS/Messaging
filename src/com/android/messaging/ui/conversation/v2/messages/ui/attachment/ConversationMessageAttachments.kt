package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationAttachmentItem
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationAttachmentSections

@Composable
internal fun ConversationMessageAttachments(
    modifier: Modifier = Modifier,
    attachmentSections: ConversationAttachmentSections,
    hasTextAboveVisualAttachments: Boolean,
    hasTextBelowVisualAttachments: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageLongClick: () -> Unit,
) {
    val hasGalleryVisualAttachments = attachmentSections.galleryVisualAttachments.isNotEmpty()
    val hasTrailingItems = attachmentSections.trailingItems.isNotEmpty()

    if (!hasGalleryVisualAttachments && !hasTrailingItems) {
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        if (hasGalleryVisualAttachments) {
            ConversationGalleryVisualAttachments(
                attachments = attachmentSections.galleryVisualAttachments,
                hasTextAboveVisualAttachments = hasTextAboveVisualAttachments,
                hasTextBelowVisualAttachments = hasTextBelowVisualAttachments,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
                onMessageLongClick = onMessageLongClick,
            )
        }

        attachmentSections.trailingItems.forEach { trailingItem ->
            when (trailingItem) {
                is ConversationAttachmentItem.Inline -> {
                    ConversationInlineAttachmentRow(
                        attachment = trailingItem.attachment,
                        onAttachmentClick = onAttachmentClick,
                        onExternalUriClick = onExternalUriClick,
                        onLongClick = onMessageLongClick,
                    )
                }

                is ConversationAttachmentItem.StandaloneVisual -> {
                    ConversationStandaloneVisualAttachment(
                        attachment = trailingItem.attachment,
                        hasTextAboveVisualAttachments = hasTextAboveVisualAttachments,
                        hasTextBelowVisualAttachments = hasTextBelowVisualAttachments,
                        onAttachmentClick = onAttachmentClick,
                        onExternalUriClick = onExternalUriClick,
                        onMessageLongClick = onMessageLongClick,
                    )
                }
            }
        }
    }
}
