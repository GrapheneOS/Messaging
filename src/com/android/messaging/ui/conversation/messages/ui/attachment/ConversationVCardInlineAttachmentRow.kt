package com.android.messaging.ui.conversation.messages.ui.attachment

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.attachment.ui.ConversationVCardAttachmentCardContent
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.preview.previewInlineVCardAttachment
import com.android.messaging.ui.core.MessagingPreviewColumn

private const val PREVIEW_LONG_CONTACT_TITLE =
    "Alexandria Cassandra Montgomery-Washington from International Partnerships"
private const val PREVIEW_LONG_CONTACT_SUBTITLE =
    "mobile +1 415 555 0198, work +1 415 555 0134, alexandria.montgomery-washington@example.com"
private const val PREVIEW_LONG_LOCATION_TITLE =
    "Northwest corner entrance near the visitor center and underground parking"
private const val PREVIEW_LONG_LOCATION_SUBTITLE =
    "1600 Amphitheatre Parkway, Building 43, Mountain View, California 94043, United States"

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
        shape = RectangleShape,
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

@PreviewLightDark
@Composable
private fun ConversationVCardInlineAttachmentRowLoadedPreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            PreviewConversationVCardInlineAttachmentRow(
                attachment = previewInlineVCardAttachment(
                    type = ConversationVCardAttachmentType.CONTACT,
                ),
            )

            PreviewConversationVCardInlineAttachmentRow(
                attachment = previewInlineVCardAttachment(
                    type = ConversationVCardAttachmentType.LOCATION,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationVCardInlineAttachmentRowLongTextPreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            PreviewConversationVCardInlineAttachmentRow(
                attachment = previewInlineVCardAttachment(
                    type = ConversationVCardAttachmentType.CONTACT,
                ).copy(
                    key = "long-contact",
                    titleText = PREVIEW_LONG_CONTACT_TITLE,
                    subtitleText = PREVIEW_LONG_CONTACT_SUBTITLE,
                ),
            )

            PreviewConversationVCardInlineAttachmentRow(
                attachment = previewInlineVCardAttachment(
                    type = ConversationVCardAttachmentType.LOCATION,
                ).copy(
                    key = "long-location",
                    titleText = PREVIEW_LONG_LOCATION_TITLE,
                    subtitleText = PREVIEW_LONG_LOCATION_SUBTITLE,
                ),
            )

            PreviewConversationVCardInlineAttachmentRow(
                attachment = previewInlineVCardAttachment(
                    type = ConversationVCardAttachmentType.CONTACT,
                ).copy(
                    key = "contact-without-subtitle",
                    titleText = "Kai",
                    subtitleText = null,
                    subtitleTextResId = null,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationVCardInlineAttachmentRowMetadataStatePreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            PreviewConversationVCardInlineAttachmentRow(
                attachment = previewInlineVCardAttachment().copy(
                    key = "missing-contact",
                    titleText = null,
                    titleTextResId = R.string.notification_vcard,
                    subtitleText = null,
                    subtitleTextResId = R.string.vcard_tap_hint,
                ),
            )

            PreviewConversationVCardInlineAttachmentRow(
                attachment = previewInlineVCardAttachment().copy(
                    key = "loading-contact",
                    titleText = null,
                    titleTextResId = R.string.notification_vcard,
                    subtitleText = null,
                    subtitleTextResId = R.string.loading_vcard,
                ),
            )

            PreviewConversationVCardInlineAttachmentRow(
                attachment = previewInlineVCardAttachment().copy(
                    key = "failed-contact",
                    titleText = null,
                    titleTextResId = R.string.notification_vcard,
                    subtitleText = null,
                    subtitleTextResId = R.string.failed_loading_vcard,
                ),
            )

            PreviewConversationVCardInlineAttachmentRow(
                attachment = previewInlineVCardAttachment(
                    type = ConversationVCardAttachmentType.LOCATION,
                ).copy(
                    key = "location-title-resource",
                    titleText = null,
                    titleTextResId = R.string.notification_location,
                    subtitleText = "Shared map pin",
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationVCardInlineAttachmentRowInteractionStatePreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            ConversationVCardInlineAttachmentRowContent(
                attachment = previewInlineVCardAttachment().copy(
                    key = "selection-mode",
                ),
                isSelectionMode = true,
                onClick = {},
                onLongClick = {},
            )

            ConversationVCardInlineAttachmentRowContent(
                attachment = previewInlineVCardAttachment().copy(
                    key = "without-open-action",
                    openAction = null,
                ),
                isSelectionMode = false,
                onClick = null,
                onLongClick = {},
            )
        }
    }
}

@Composable
private fun PreviewConversationVCardInlineAttachmentRow(
    attachment: ConversationInlineAttachment.VCard,
) {
    ConversationVCardInlineAttachmentRow(
        attachment = attachment,
        isSelectionMode = false,
        onAttachmentClick = { _, _ -> },
        onExternalUriClick = {},
        onLongClick = {},
    )
}
