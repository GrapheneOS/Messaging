package com.android.messaging.ui.shareintent.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.common.components.ParticipantAvatar
import com.android.messaging.ui.common.components.TwoLineListItem
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState

@Composable
internal fun NewMessageItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShareTargetRow(
        title = stringResource(R.string.share_new_message),
        subtitle = null,
        avatarUri = null,
        fallbackIcon = Icons.Default.Edit,
        isSelected = false,
        onClick = onClick,
        onLongClick = null,
        modifier = modifier,
    )
}

@Composable
internal fun ShareTargetItem(
    target: ShareTargetUiState,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val fallbackIcon = when (target) {
        is ShareTargetUiState.Conversation -> when {
            target.isGroup -> Icons.Default.Group
            else -> Icons.Default.Person
        }

        is ShareTargetUiState.Contact -> Icons.Default.Person
    }

    ShareTargetRow(
        title = target.displayName,
        subtitle = target.details,
        avatarUri = target.avatarUri,
        fallbackIcon = fallbackIcon,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    )
}

@Composable
private fun ShareTargetRow(
    title: String,
    subtitle: String?,
    avatarUri: String?,
    fallbackIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.background
    }

    TwoLineListItem(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        color = backgroundColor,
        leadingContent = {
            ParticipantAvatar(
                avatarUri = avatarUri,
                size = AvatarSize,
                fallbackIconSize = FallbackIconSize,
                fallbackIcon = fallbackIcon,
                isSelected = isSelected,
            )
        },
    )
}

@PreviewLightDark
@Composable
private fun ShareTargetItemPreview() {
    MessagingPreviewColumn {
        ShareTargetItem(
            target = ShareTargetUiState.Conversation(
                conversationId = "1",
                normalizedDestination = "+31612345678",
                displayName = "Jane Doe",
                details = "+31 6 1234 5678",
                avatarUri = null,
                isGroup = false,
            ),
            isSelected = false,
            onClick = {},
            onLongClick = null,
        )
    }
}

@PreviewLightDark
@Composable
private fun NewMessageItemPreview() {
    MessagingPreviewColumn {
        NewMessageItem(onClick = {})
    }
}
