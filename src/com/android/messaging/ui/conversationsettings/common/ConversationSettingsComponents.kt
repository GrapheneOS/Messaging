package com.android.messaging.ui.conversationsettings.common

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.ParticipantAvatar
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import com.android.messaging.ui.core.AppTheme

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
            fallbackIconSize = 64.dp,
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
    shape: Shape = RoundedCornerShape(20.dp),
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
            )
        }
    }
}

@Composable
internal fun ParticipantItem(
    participant: ParticipantUiState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ParticipantAvatar(
            avatarUri = participant.avatarUri.takeUnless { participant.isBlocked },
            modifier = Modifier.size(40.dp),
            fallbackIcon = when {
                participant.isBlocked -> Icons.Default.Block
                else -> Icons.Default.Person
            },
            fallbackIconSize = 24.dp,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = participant.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (!participant.details.isNullOrEmpty()) {
                Text(
                    text = participant.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (onAction != null) {
            IconButton(onClick = onAction) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.action_contact_info),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ConversationHeaderPreview() {
    AppTheme {
        ConversationHeader(
            title = "Mother",
            participant = ParticipantUiState(
                avatarUri = null,
                displayName = "Mother",
                details = "+31 6 1234 5678",
                contactId = 1L,
                lookupKey = null,
                normalizedDestination = "+31612345678",
                isBlocked = false,
                displayDestination = "+31 6 1234 5678",
            ),
        )
    }
}

@Preview
@Composable
private fun ConversationSettingsItemPreview() {
    AppTheme {
        ConversationSettingsItem(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun ParticipantItemPreview() {
    AppTheme {
        ParticipantItem(
            participant = ParticipantUiState(
                avatarUri = null,
                displayName = "Mother",
                details = "+31 6 1234 5678",
                contactId = 1L,
                lookupKey = null,
                normalizedDestination = "+31612345678",
                isBlocked = false,
                displayDestination = "+31 6 1234 5678",
            ),
            onClick = {},
            onLongClick = {},
            onAction = {},
        )
    }
}
