package com.android.messaging.ui.blockedparticipants.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntRect
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
    var avatarBoundsPx by remember { mutableStateOf(IntRect.Zero) }
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
        onAvatarBoundsChanged = { avatarBoundsPx = it },
        modifier = modifier,
    )

    BlockedParticipantQuickActions(
        visible = showQuickActions,
        anchorBoundsPx = avatarBoundsPx,
        participant = participant,
        fallbackIcon = fallbackIcon,
        onDismiss = dismissPopup,
        onMessageClick = onMessageClick,
        onCallClick = onCallClick,
        onContactClick = onContactClick,
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
    onAvatarBoundsChanged: (IntRect) -> Unit,
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
            BlockedParticipantAvatarSlot(
                avatarUri = participant.avatarUri,
                fallbackIcon = fallbackIcon,
                isSelected = isSelected,
                onClick = onAvatarClick,
                onBoundsChanged = onAvatarBoundsChanged,
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
    anchorBoundsPx: IntRect,
    participant: BlockedParticipantUiState,
    fallbackIcon: ImageVector,
    onDismiss: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
) {
    ParticipantQuickActionsPopup(
        visible = visible,
        anchorBoundsPx = anchorBoundsPx,
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
private fun BlockedParticipantAvatarSlot(
    avatarUri: String?,
    fallbackIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    onBoundsChanged: (IntRect) -> Unit,
) {
    ParticipantAvatar(
        avatarUri = avatarUri,
        size = 48.dp,
        fallbackIcon = fallbackIcon,
        isSelected = isSelected,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                enabled = !isSelected,
                onClick = onClick
            )
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                onBoundsChanged(
                    IntRect(
                        left = pos.x.toInt(),
                        top = pos.y.toInt(),
                        right = pos.x.toInt() + coords.size.width,
                        bottom = pos.y.toInt() + coords.size.height,
                    ),
                )
            },
    )
}

@Composable
private fun BlockedParticipantInfo(
    displayName: String,
    details: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = details.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
