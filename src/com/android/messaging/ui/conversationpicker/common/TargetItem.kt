package com.android.messaging.ui.conversationpicker.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.ui.common.components.ParticipantAvatar
import com.android.messaging.ui.common.components.TwoLineListItem
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import com.android.messaging.ui.core.MessagingPreviewColumn

@Composable
internal fun TargetItem(
    target: TargetUiState,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val avatarContent = target.avatarContent()

    TargetRow(
        title = target.displayName,
        subtitle = target.details,
        avatarUri = target.avatarUri,
        colorSeedCode = avatarContent.colorSeedCode,
        fallbackIcon = avatarContent.fallbackIcon,
        fallbackLabel = avatarContent.fallbackLabel,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    )
}

@Composable
private fun TargetRow(
    title: String,
    subtitle: String?,
    avatarUri: String?,
    colorSeedCode: String?,
    fallbackIcon: ImageVector,
    fallbackLabel: String?,
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
                fallbackLabel = fallbackLabel,
                colorSeedCode = colorSeedCode,
                fallbackSize = FallbackSize,
                fallbackIcon = fallbackIcon,
                isSelected = isSelected,
            )
        },
    )
}

@PreviewLightDark
@Composable
private fun TargetItemPreview() {
    MessagingPreviewColumn {
        TargetItem(
            target = TargetUiState.Conversation(
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
