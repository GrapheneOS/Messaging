package com.android.messaging.ui.conversationlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.ui.common.components.TwoLineListItem
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListPreviewUiModel
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.util.Dates

@Composable
internal fun ConversationListItemRow(
    item: ConversationListItemUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    onAvatarMessageClick: () -> Unit = {},
    onAvatarCallClick: (() -> Unit)? = null,
    onAvatarContactClick: (() -> Unit)? = null,
    onAvatarInfoClick: () -> Unit = {},
) {
    TwoLineListItem(
        onClick = onClick,
        leadingContent = {
            ConversationListItemAvatar(
                item = item,
                isSelectionMode = isSelectionMode,
                onToggleSelection = onClick,
                onMessageClick = onAvatarMessageClick,
                onCallClick = onAvatarCallClick,
                onContactClick = onAvatarContactClick,
                onInfoClick = onAvatarInfoClick,
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
        contentDescription = conversationListItemContentDescription(item),
        keepLeadingContentAccessible = true,
        subtitleContent = {
            ConversationListItemBody(item)
        },
    )
}

@Composable
private fun ConversationListItemHeader(item: ConversationListItemUiModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ItemHeaderSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(ItemBadgeSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(
                    weight = 1f,
                    fill = false,
                ),
                text = item.title.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = itemUnreadFontWeight(item),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            ConversationListItemBadges(item)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(ItemBadgeSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConversationListItemStatusLabel(item)

            if (item.isUnread) {
                ConversationListItemStatusDot(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ConversationListItemBadges(item: ConversationListItemUiModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ItemBadgeSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.isEnterprise) {
            ConversationListItemBadgeIcon(Icons.Default.Work)
        }

        when {
            item.isMuted -> ConversationListItemBadgeIcon(Icons.Default.NotificationsOff)
            item.isSnoozed -> ConversationListItemBadgeIcon(Icons.Default.NotificationsPaused)
        }

        if (item.isPinned) {
            ConversationListItemBadgeIcon(Icons.Default.PushPin)
        }
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
        ConversationListItemSnippet(
            item = item,
            text = snippetText,
            fontWeight = itemUnreadFontWeight(item),
            fontStyle = FontStyle.Italic.takeIf { item.snippet.isDraft },
            color = itemSnippetColor(item),
        )
    }
}

@Composable
private fun ConversationListItemStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ItemStatusDotSize)
            .clip(CircleShape)
            .background(color),
    )
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
private fun ConversationListItemStatusLabel(item: ConversationListItemUiModel) {
    val text = when (item.status) {
        ConversationListMessageStatus.Draft,
        ConversationListMessageStatus.Unknown,
        -> {
            stringResource(R.string.conversation_list_item_view_draft_message)
        }

        ConversationListMessageStatus.Sending -> {
            stringResource(R.string.message_status_sending)
        }

        else -> {
            remember(item.timestampMillis) {
                Dates.getConversationTimeString(item.timestampMillis).toString()
            }
        }
    }

    val color = when {
        item.isUnread -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontStyle = FontStyle.Italic.takeIf { item.snippet.isDraft },
        color = color,
        maxLines = 1,
    )
}

@Composable
private fun itemSnippetText(item: ConversationListItemUiModel): String? {
    val snippetText = item.snippet.text?.takeIf(String::isNotBlank)

    if (snippetText != null) {
        val senderName = item.snippet.senderName?.takeIf(String::isNotBlank)

        return when {
            item.avatar.isGroup && !item.isOutgoing && senderName != null -> {
                "$senderName: $snippetText"
            }

            else -> snippetText
        }
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

        is ConversationListPreviewUiModel.File -> {
            stringResource(R.string.mms_text)
        }

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
        item.isUnread -> FontWeight.Medium
        else -> FontWeight.Normal
    }
}

@Composable
private fun itemSnippetColor(item: ConversationListItemUiModel): Color {
    return when {
        item.status is ConversationListMessageStatus.Failed -> MaterialTheme.colorScheme.error
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
                    conversationId = "snoozed",
                    title = "Liam Carter",
                    snippetText = "Let's catch up later",
                    isSnoozed = true,
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
                    preview = ConversationListPreviewUiModel.Audio,
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
