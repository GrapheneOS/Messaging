package com.android.messaging.ui.shareintent.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.messaging.R
import com.android.messaging.ui.common.components.ParticipantAvatar
import com.android.messaging.ui.common.components.TwoLineListItem
import com.android.messaging.ui.core.AppTheme
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
        fallbackIcon = Icons.Outlined.Edit,
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
    ShareTargetRow(
        title = target.displayName,
        subtitle = target.details,
        avatarUri = target.avatarUri,
        fallbackIcon = null,
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
    fallbackIcon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    TwoLineListItem(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        leadingContent = {
            if (fallbackIcon != null) {
                ParticipantAvatar(
                    avatarUri = avatarUri,
                    size = AvatarSize,
                    fallbackIconSize = FallbackIconSize,
                    fallbackIcon = fallbackIcon,
                    isSelected = isSelected,
                )
            } else {
                ParticipantAvatar(
                    avatarUri = avatarUri,
                    size = AvatarSize,
                    isSelected = isSelected,
                )
            }
        },
    )
}

@Preview
@Composable
private fun ShareTargetItemPreview() {
    AppTheme {
        ShareTargetItem(
            target = ShareTargetUiState(
                conversationId = "1",
                displayName = "Jane Doe",
                details = "+31 6 1234 5678",
                avatarUri = null,
            ),
            isSelected = false,
            onClick = {},
            onLongClick = null,
        )
    }
}

@Preview
@Composable
private fun NewMessageItemPreview() {
    AppTheme {
        NewMessageItem(onClick = {})
    }
}
