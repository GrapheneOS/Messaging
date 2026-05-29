package com.android.messaging.ui.blockedparticipants.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantUiState
import com.android.messaging.ui.common.components.ParticipantAvatar
import com.android.messaging.ui.common.components.ParticipantQuickActionsPopup
import com.android.messaging.ui.core.AppTheme

@Composable
internal fun BlockedParticipantItem(
    participant: BlockedParticipantUiState,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onUnblockClick: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var showQuickActions by remember { mutableStateOf(false) }
    val fallbackIcon = Icons.Default.Person
    val dismissPopup = { showQuickActions = false }

    BlockedParticipantRow(
        participant = participant,
        isSelected = isSelected,
        fallbackIcon = fallbackIcon,
        onClick = onClick,
        onLongClick = onLongClick,
        onUnblockClick = onUnblockClick,
        onAvatarClick = { showQuickActions = true },
        quickActionsVisible = showQuickActions,
        onDismissQuickActions = dismissPopup,
        onMessageClick = onMessageClick,
        onCallClick = onCallClick,
        onContactClick = onContactClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BlockedParticipantRow(
    participant: BlockedParticipantUiState,
    isSelected: Boolean,
    fallbackIcon: ImageVector,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onUnblockClick: () -> Unit,
    onAvatarClick: () -> Unit,
    quickActionsVisible: Boolean,
    onDismissQuickActions: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.background
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.blockedParticipantItemShape,
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(
                    horizontal = ItemHorizontalPadding,
                    vertical = ItemVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BlockedParticipantAvatarWithQuickActions(
                participant = participant,
                fallbackIcon = fallbackIcon,
                isSelected = isSelected,
                onAvatarClick = onAvatarClick,
                quickActionsVisible = quickActionsVisible,
                onDismissQuickActions = onDismissQuickActions,
                onMessageClick = onMessageClick,
                onCallClick = onCallClick,
                onContactClick = onContactClick,
            )

            Spacer(modifier = Modifier.width(ItemHorizontalPadding))

            BlockedParticipantInfo(
                displayName = participant.displayName,
                details = participant.details,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = ItemHorizontalPadding),
            )

            UnblockButton(onClick = onUnblockClick)
        }
    }
}

@Composable
private fun BlockedParticipantQuickActions(
    visible: Boolean,
    participant: BlockedParticipantUiState,
    fallbackIcon: ImageVector,
    onDismiss: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
) {
    ParticipantQuickActionsPopup(
        visible = visible,
        avatarUri = participant.avatarUri,
        displayName = participant.displayName,
        subtitle = participant.details,
        fallbackIcon = fallbackIcon,
        onDismiss = onDismiss,
        onMessageClick = {
            onMessageClick()
            onDismiss()
        },
        onCallClick = {
            onCallClick?.invoke()
            onDismiss()
        }.takeIf { onCallClick != null },
        onContactClick = {
            onContactClick?.invoke()
            onDismiss()
        }.takeIf { onContactClick != null },
        isContactSaved = participant.isContactSaved,
    )
}

@Composable
private fun BlockedParticipantAvatarWithQuickActions(
    participant: BlockedParticipantUiState,
    fallbackIcon: ImageVector,
    isSelected: Boolean,
    onAvatarClick: () -> Unit,
    quickActionsVisible: Boolean,
    onDismissQuickActions: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
) {
    Box(modifier = Modifier.size(48.dp)) {
        ParticipantAvatar(
            avatarUri = participant.avatarUri,
            size = 48.dp,
            fallbackIcon = fallbackIcon,
            isSelected = isSelected,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    enabled = !isSelected,
                    onClick = onAvatarClick
                ),
        )

        BlockedParticipantQuickActions(
            visible = quickActionsVisible,
            participant = participant,
            fallbackIcon = fallbackIcon,
            onDismiss = onDismissQuickActions,
            onMessageClick = onMessageClick,
            onCallClick = onCallClick,
            onContactClick = onContactClick,
        )
    }
}

@Composable
private fun BlockedParticipantInfo(
    displayName: String,
    details: String?,
    modifier: Modifier = Modifier,
) {
    val twoLineHeight = with(LocalDensity.current) {
        MaterialTheme.typography.bodyLarge.lineHeight.toDp() +
            MaterialTheme.typography.bodySmall.lineHeight.toDp()
    }

    Column(
        modifier = modifier.heightIn(min = twoLineHeight),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (!details.isNullOrBlank()) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UnblockButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.PersonRemove,
            contentDescription = stringResource(R.string.tap_to_unblock_message),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun BlockedParticipantItemPreview() {
    AppTheme {
        BlockedParticipantItem(
            participant = BlockedParticipantUiState(
                participantId = "1",
                conversationId = "c1",
                avatarUri = null,
                displayName = "Spam Caller",
                details = "+31 6 1234 5678",
                contactId = 1L,
                lookupKey = null,
                normalizedDestination = "+31612345678",
                canCall = true,
                isContactSaved = true,
            ),
            isSelected = false,
            onClick = {},
            onLongClick = {},
            onUnblockClick = {},
            onMessageClick = {},
            onCallClick = {},
            onContactClick = {},
        )
    }
}
