package com.android.messaging.ui.conversationsettings.common

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.ParticipantAvatar
import com.android.messaging.ui.common.components.ParticipantQuickActionsPopup
import com.android.messaging.ui.common.components.participantAvatarLabel
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import com.android.messaging.ui.core.MessagingPreviewColumn

@Composable
internal fun ConversationHeader(
    title: String,
    participant: ParticipantUiState?,
    modifier: Modifier = Modifier,
    collapseProgress: () -> Float = { 0f },
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val isBlocked = participant?.isBlocked == true

        ParticipantAvatar(
            avatarUri = participant?.avatarUri.takeUnless { isBlocked },
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer {
                    val scale = 1f - collapseProgress()
                    scaleX = scale
                    scaleY = scale
                },
            fallbackIcon = when {
                isBlocked -> Icons.Default.Block
                participant == null -> Icons.Default.Group
                else -> Icons.Default.Person
            },
            fallbackSize = 64.dp,
            fallbackLabel = participantAvatarLabel(
                source = participant?.displayName
            ).takeUnless { isBlocked },
        )

        if (title.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer {
                    alpha = 1f - collapseProgress()
                },
            )
        }
    }
}

@Composable
internal fun ConversationSettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = MaterialTheme.settingsCardShape,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 18.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ParticipantItem(
    participant: ParticipantUiState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var showQuickActions by remember { mutableStateOf(false) }
    val isBlocked = participant.isBlocked
    val fallbackIcon = if (isBlocked) Icons.Default.Block else Icons.Default.Person
    val avatarUri = participant.avatarUri.takeUnless { isBlocked }
    val dismissPopup = { showQuickActions = false }
    val fallbackLabel = participantAvatarLabel(
        source = participant.displayName
    ).takeUnless { isBlocked }

    ParticipantRow(
        participant = participant,
        avatarUri = avatarUri,
        fallbackIcon = fallbackIcon,
        fallbackLabel = fallbackLabel,
        onRowClick = { showQuickActions = true },
        onLongClick = onLongClick,
        onAction = onAction,
        quickActionsVisible = showQuickActions,
        onDismissQuickActions = dismissPopup,
        onMessageClick = onClick,
        onCallClick = onCallClick,
        onContactClick = onContactClick,
        modifier = modifier,
    )
}

@Composable
private fun ParticipantRow(
    participant: ParticipantUiState,
    avatarUri: String?,
    fallbackIcon: ImageVector,
    fallbackLabel: String?,
    onRowClick: () -> Unit,
    onLongClick: () -> Unit,
    onAction: (() -> Unit)?,
    quickActionsVisible: Boolean,
    onDismissQuickActions: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onRowClick,
                onLongClick = onLongClick,
            )
            .padding(
                start = 16.dp,
                top = 8.dp,
                end = 2.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ParticipantAvatarWithQuickActions(
            participant = participant,
            avatarUri = avatarUri,
            fallbackIcon = fallbackIcon,
            fallbackLabel = fallbackLabel,
            quickActionsVisible = quickActionsVisible,
            onDismissQuickActions = onDismissQuickActions,
            onMessageClick = onMessageClick,
            onCallClick = onCallClick,
            onContactClick = onContactClick,
        )

        Spacer(modifier = Modifier.width(16.dp))

        ParticipantInfo(
            displayName = participant.displayName,
            details = participant.details,
            modifier = Modifier.weight(1f),
        )

        if (onAction != null) {
            ParticipantInfoButton(onClick = onAction)
        }
    }
}

@Composable
private fun ParticipantQuickActions(
    visible: Boolean,
    participant: ParticipantUiState,
    avatarUri: String?,
    fallbackIcon: ImageVector,
    fallbackLabel: String?,
    onDismiss: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
) {
    ParticipantQuickActionsPopup(
        visible = visible,
        avatarUri = avatarUri,
        displayName = participant.displayName,
        subtitle = participant.details,
        fallbackIcon = fallbackIcon,
        fallbackLabel = fallbackLabel,
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
private fun ParticipantAvatarWithQuickActions(
    participant: ParticipantUiState,
    avatarUri: String?,
    fallbackIcon: ImageVector,
    fallbackLabel: String?,
    quickActionsVisible: Boolean,
    onDismissQuickActions: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
) {
    Box(modifier = Modifier.size(40.dp)) {
        ParticipantAvatar(
            avatarUri = avatarUri,
            fallbackIcon = fallbackIcon,
            fallbackSize = 24.dp,
            fallbackLabel = fallbackLabel,
            modifier = Modifier.matchParentSize(),
        )

        ParticipantQuickActions(
            visible = quickActionsVisible,
            participant = participant,
            avatarUri = avatarUri,
            fallbackIcon = fallbackIcon,
            fallbackLabel = fallbackLabel,
            onDismiss = onDismissQuickActions,
            onMessageClick = onMessageClick,
            onCallClick = onCallClick,
            onContactClick = onContactClick,
        )
    }
}

@Composable
private fun ParticipantInfo(
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

        if (!details.isNullOrEmpty()) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ParticipantInfoButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = stringResource(R.string.action_contact_info),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationHeaderPreview() {
    MessagingPreviewColumn {
        ConversationHeader(
            title = "Mother",
            participant = ParticipantUiState(
                id = "preview",
                avatarUri = null,
                displayName = "Mother",
                details = "+31 6 1234 5678",
                contactId = 1L,
                lookupKey = null,
                normalizedDestination = "+31612345678",
                isBlocked = false,
                displayDestination = "+31 6 1234 5678",
                canCall = true,
                isContactSaved = true,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationSettingsItemPreview() {
    MessagingPreviewColumn {
        ConversationSettingsItem(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            onClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ParticipantItemPreview() {
    MessagingPreviewColumn {
        ParticipantItem(
            participant = ParticipantUiState(
                id = "preview",
                avatarUri = null,
                displayName = "Mother",
                details = "+31 6 1234 5678",
                contactId = 1L,
                lookupKey = null,
                normalizedDestination = "+31612345678",
                isBlocked = false,
                displayDestination = "+31 6 1234 5678",
                canCall = true,
                isContactSaved = true,
            ),
            onClick = {},
            onLongClick = {},
            onAction = {},
            onCallClick = {},
            onContactClick = {},
        )
    }
}
