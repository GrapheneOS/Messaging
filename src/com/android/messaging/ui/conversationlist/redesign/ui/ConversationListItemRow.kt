package com.android.messaging.ui.conversationlist.redesign.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.ui.common.components.TwoLineListItem
import com.android.messaging.ui.common.components.attachment.MediaThumbnail
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListPreviewUiModel
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.util.Dates

@Composable
internal fun ConversationListItemRow(
    item: ConversationListItemUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
) {
    TwoLineListItem(
        onClick = onClick,
        leadingContent = {
            ConversationListItemAvatar(
                item = item,
                isSelectionMode = isSelectionMode,
                onToggleSelection = onClick,
            )
        },
        titleContent = {
            ConversationListItemHeader(item)
        },
        modifier = modifier.testTag(
            tag = conversationListItemTestTag(item.conversationId),
        ),
        onLongClick = onLongClick,
        color = itemContainerColor(item),
        subtitleContent = {
            ConversationListItemBody(item)
        },
        trailingContent = {
            ConversationListItemTrailing(item)
        },
    )
}

@Composable
private fun ConversationListItemHeader(item: ConversationListItemUiModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ItemHeaderSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = item.title.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = itemUnreadFontWeight(item),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (item.isEnterprise) {
            ConversationListItemBadgeIcon(Icons.Default.Work)
        }

        if (item.isMuted) {
            ConversationListItemBadgeIcon(Icons.Default.NotificationsOff)
        }

        ConversationListItemStatusLabel(item)
    }
}

@Composable
private fun ConversationListItemBody(item: ConversationListItemUiModel) {
    item.subject?.let { subject ->
        Text(
            text = subject,
            style = MaterialTheme.typography.titleSmall,
            color = itemSnippetColor(item),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    itemSnippetText(item)?.let { snippetText ->
        Text(
            text = snippetText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = itemUnreadFontWeight(item),
            color = itemSnippetColor(item),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ConversationListItemTrailing(item: ConversationListItemUiModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ItemHeaderSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.isUnread) {
            ConversationListItemUnreadDot()
        }

        ConversationListItemPreviewThumbnail(item.snippet.preview)
    }
}

@Composable
private fun ConversationListItemBadgeIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(ItemBadgeIconSize),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ConversationListItemUnreadDot() {
    Box(
        modifier = Modifier
            .size(ItemUnreadDotSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    )
}

@Composable
private fun ConversationListItemPreviewThumbnail(preview: ConversationListPreviewUiModel?) {
    val isVisual = preview is ConversationListPreviewUiModel.Image ||
        preview is ConversationListPreviewUiModel.Video

    if (preview == null || !isVisual) {
        return
    }

    val thumbnailSizePx = with(LocalDensity.current) {
        ItemPreviewThumbnailSize.roundToPx()
    }

    MediaThumbnail(
        modifier = Modifier
            .size(ItemPreviewThumbnailSize)
            .clip(ItemPreviewThumbnailShape),
        contentUri = preview.contentUri,
        contentType = preview.contentType,
        size = IntSize(
            width = thumbnailSizePx,
            height = thumbnailSizePx,
        ),
    )
}

@Composable
private fun ConversationListItemStatusLabel(item: ConversationListItemUiModel) {
    val status = item.status

    val text = when (status) {
        ConversationListMessageStatus.Draft,
        ConversationListMessageStatus.Unknown,
        -> {
            stringResource(R.string.conversation_list_item_view_draft_message)
        }

        ConversationListMessageStatus.Sending -> {
            stringResource(R.string.message_status_sending)
        }

        is ConversationListMessageStatus.Failed -> {
            stringResource(itemFailedStatusResId(item))
        }

        ConversationListMessageStatus.Normal -> {
            remember(item.timestampMillis) {
                Dates.getConversationTimeString(item.timestampMillis).toString()
            }
        }
    }

    val color = when {
        status is ConversationListMessageStatus.Failed -> MaterialTheme.colorScheme.error
        item.isUnread -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
    )
}

private fun itemFailedStatusResId(item: ConversationListItemUiModel): Int {
    return when {
        item.isOutgoing -> R.string.conversation_list_status_not_sent
        else -> R.string.conversation_list_status_not_downloaded
    }
}

@Composable
private fun itemSnippetText(item: ConversationListItemUiModel): String? {
    val snippetText = item.snippet.text?.takeIf(String::isNotBlank)

    if (snippetText != null) {
        return snippetText
    }

    return when (item.snippet.preview) {
        is ConversationListPreviewUiModel.Audio -> {
            stringResource(R.string.conversation_list_snippet_audio_clip)
        }

        is ConversationListPreviewUiModel.Image -> {
            stringResource(R.string.conversation_list_snippet_picture)
        }

        is ConversationListPreviewUiModel.Video -> {
            stringResource(R.string.conversation_list_snippet_video)
        }

        is ConversationListPreviewUiModel.VCard -> {
            stringResource(R.string.conversation_list_snippet_vcard)
        }

        is ConversationListPreviewUiModel.File -> stringResource(R.string.mms_text)

        null -> null
    }
}

@Composable
private fun itemContainerColor(item: ConversationListItemUiModel): Color {
    return when {
        item.isSelected -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.background
    }
}

private fun itemUnreadFontWeight(item: ConversationListItemUiModel): FontWeight {
    return when {
        item.isUnread -> FontWeight.Bold
        else -> FontWeight.Normal
    }
}

@Composable
private fun itemSnippetColor(item: ConversationListItemUiModel): Color {
    return when {
        item.isUnread -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@PreviewLightDark
@Composable
private fun ConversationListItemRowPreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 2.dp)) {
            ConversationListItemRow(
                item = previewConversationListItem(
                    conversationId = "unread",
                    title = "Jane Doe",
                    snippetText = "Are we still on for tomorrow?",
                    isUnread = true,
                ),
                onClick = {},
                onLongClick = {},
            )
            ConversationListItemRow(
                item = previewConversationListItem(
                    conversationId = "read",
                    title = "Ada Lovelace",
                    snippetText = "Sounds good, thanks!",
                ),
                onClick = {},
                onLongClick = {},
            )
            ConversationListItemRow(
                item = previewConversationListItem(
                    conversationId = "draft",
                    title = "Grace Hopper",
                    snippetText = "I was thinking that we could",
                    status = ConversationListMessageStatus.Draft,
                    isDraft = true,
                ),
                onClick = {},
                onLongClick = {},
            )
            ConversationListItemRow(
                item = previewConversationListItem(
                    conversationId = "sending",
                    title = "Amelia Brown",
                    snippetText = "On my way!",
                    status = ConversationListMessageStatus.Sending,
                    isOutgoing = true,
                ),
                onClick = {},
                onLongClick = {},
            )
            ConversationListItemRow(
                item = previewConversationListItem(
                    conversationId = "failed",
                    title = "Marina Silva",
                    snippetText = "Did you get my last message?",
                    status = ConversationListMessageStatus.Failed(rawTelephonyStatus = 0),
                    isOutgoing = true,
                ),
                onClick = {},
                onLongClick = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationListItemRowBadgesPreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 2.dp)) {
            ConversationListItemRow(
                item = previewConversationListItem(
                    conversationId = "muted_group",
                    title = "Weekend plans",
                    snippetText = "Jane: I can bring snacks",
                    isGroup = true,
                    isMuted = true,
                    isUnread = true,
                ),
                onClick = {},
                onLongClick = {},
            )
            ConversationListItemRow(
                item = previewConversationListItem(
                    conversationId = "enterprise",
                    title = "Alex Appleseed",
                    snippetText = "The report is ready for review",
                    isEnterprise = true,
                ),
                onClick = {},
                onLongClick = {},
            )
            ConversationListItemRow(
                item = previewConversationListItem(
                    conversationId = "selected",
                    title = "Brian Cohen",
                    snippetText = "Happy birthday!",
                    isSelected = true,
                ),
                onClick = {},
                onLongClick = {},
            )
            ConversationListItemRow(
                item = previewConversationListItem(
                    conversationId = "subject",
                    title = "Maria Tamm",
                    snippetText = "Check out the details below",
                    subject = "Meeting agenda",
                ),
                onClick = {},
                onLongClick = {},
            )
            ConversationListItemRow(
                item = previewConversationListItem(
                    conversationId = "audio",
                    title = "Sara Lindberg",
                    snippetText = null,
                    preview = ConversationListPreviewUiModel.Audio(
                        contentUri = "content://preview/audio",
                        contentType = "audio/mp3",
                    ),
                ),
                onClick = {},
                onLongClick = {},
            )
            ConversationListItemRow(
                item = previewConversationListItem(
                    conversationId = "picture",
                    title = "Tomas Kask",
                    snippetText = null,
                    isUnread = true,
                    preview = ConversationListPreviewUiModel.Image(
                        contentUri = "content://preview/image",
                        contentType = "image/jpeg",
                    ),
                ),
                onClick = {},
                onLongClick = {},
            )
        }
    }
}
