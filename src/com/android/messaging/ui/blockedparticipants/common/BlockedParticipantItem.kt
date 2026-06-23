package com.android.messaging.ui.blockedparticipants.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantUiState
import com.android.messaging.ui.common.components.TwoLineListItem
import com.android.messaging.ui.common.components.participant.ParticipantAvatar
import com.android.messaging.ui.common.components.participant.ParticipantQuickActionsPopup
import com.android.messaging.ui.common.components.participant.participantAvatarLabel
import com.android.messaging.ui.common.components.participant.participantColorSeed
import com.android.messaging.ui.core.MessagingPreviewColumn

@Composable
internal fun BlockedParticipantItem(
    participant: BlockedParticipantUiState,
    isSelected: Boolean,
    inSelectionMode: Boolean,
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
    val onAvatarClick = {
        when {
            inSelectionMode -> onClick()
            else -> showQuickActions = true
        }
    }

    BlockedParticipantRow(
        participant = participant,
        isSelected = isSelected,
        inSelectionMode = inSelectionMode,
        fallbackIcon = fallbackIcon,
        onClick = onClick,
        onLongClick = onLongClick,
        onUnblockClick = onUnblockClick,
        onAvatarClick = onAvatarClick,
        quickActionsVisible = showQuickActions,
        onDismissQuickActions = dismissPopup,
        onMessageClick = onMessageClick,
        onCallClick = onCallClick,
        onContactClick = onContactClick,
        modifier = modifier,
    )
}

@Composable
private fun BlockedParticipantRow(
    participant: BlockedParticipantUiState,
    isSelected: Boolean,
    inSelectionMode: Boolean,
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

    TwoLineListItem(
        title = participant.displayName,
        subtitle = participant.details,
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        color = backgroundColor,
        leadingContent = {
            BlockedParticipantAvatarWithQuickActions(
                participant = participant,
                fallbackIcon = fallbackIcon,
                isSelected = isSelected,
                inSelectionMode = inSelectionMode,
                onAvatarClick = onAvatarClick,
                quickActionsVisible = quickActionsVisible,
                onDismissQuickActions = onDismissQuickActions,
                onMessageClick = onMessageClick,
                onCallClick = onCallClick,
                onContactClick = onContactClick,
            )
        },
        trailingContent = {
            UnblockButton(onClick = onUnblockClick)
        },
    )
}

@Composable
private fun BlockedParticipantQuickActions(
    visible: Boolean,
    participant: BlockedParticipantUiState,
    fallbackIcon: ImageVector,
    colorSeedCode: String?,
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
        fallbackLabel = participantAvatarLabel(source = participant.displayName),
        colorSeedCode = colorSeedCode,
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
        onInfoClick = null,
        isContactSaved = participant.isContactSaved,
    )
}

@Composable
private fun BlockedParticipantAvatarWithQuickActions(
    participant: BlockedParticipantUiState,
    fallbackIcon: ImageVector,
    isSelected: Boolean,
    inSelectionMode: Boolean,
    onAvatarClick: () -> Unit,
    quickActionsVisible: Boolean,
    onDismissQuickActions: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
) {
    val colorSeedCode = participantColorSeed(
        normalizedDestination = participant.normalizedDestination,
    )

    Box(modifier = Modifier.size(48.dp)) {
        ParticipantAvatar(
            avatarUri = participant.avatarUri,
            size = 48.dp,
            fallbackLabel = participantAvatarLabel(source = participant.displayName),
            fallbackIcon = fallbackIcon,
            colorSeedCode = colorSeedCode,
            isSelected = isSelected,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    enabled = inSelectionMode || !isSelected,
                    onClick = onAvatarClick
                ),
        )

        BlockedParticipantQuickActions(
            visible = quickActionsVisible && !inSelectionMode,
            participant = participant,
            fallbackIcon = fallbackIcon,
            colorSeedCode = colorSeedCode,
            onDismiss = onDismissQuickActions,
            onMessageClick = onMessageClick,
            onCallClick = onCallClick,
            onContactClick = onContactClick,
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

@PreviewLightDark
@Composable
private fun BlockedParticipantItemPreview() {
    MessagingPreviewColumn {
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
                canShowContact = true,
                isContactSaved = true,
            ),
            isSelected = false,
            inSelectionMode = false,
            onClick = {},
            onLongClick = {},
            onUnblockClick = {},
            onMessageClick = {},
            onCallClick = {},
            onContactClick = {},
        )
    }
}
