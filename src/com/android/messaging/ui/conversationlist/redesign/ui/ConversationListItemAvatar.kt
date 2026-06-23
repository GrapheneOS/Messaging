package com.android.messaging.ui.conversationlist.redesign.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.messaging.ui.common.components.participant.ParticipantQuickActionsPopup
import com.android.messaging.ui.common.components.participant.participantAvatarLabel
import com.android.messaging.ui.common.components.participant.participantColorSeed
import com.android.messaging.ui.common.components.selection.SelectionListAvatar
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListItemUiModel

@Composable
internal fun ConversationListItemAvatar(
    item: ConversationListItemUiModel,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
    onInfoClick: () -> Unit,
) {
    val fallbackIcon = when {
        item.avatar.isGroup -> Icons.Default.Group
        else -> Icons.Default.Person
    }

    val fallbackLabel = when {
        item.avatar.isGroup -> null
        else -> participantAvatarLabel(source = item.title)
    }

    val colorSeedCode = participantColorSeed(
        normalizedDestination = item.avatar.normalizedDestination,
    )

    var showQuickActions by remember { mutableStateOf(false) }

    Box(modifier = Modifier.size(ItemAvatarSize)) {
        SelectionListAvatar(
            avatarUri = item.avatar.uri,
            size = ItemAvatarSize,
            fallbackLabel = fallbackLabel,
            colorSeedCode = colorSeedCode,
            fallbackIcon = fallbackIcon,
            isSelected = item.isSelected,
            modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    if (isSelectionMode) {
                        onToggleSelection()
                    } else {
                        showQuickActions = true
                    }
                },
        )

        ConversationListAvatarQuickActions(
            item = item,
            visible = showQuickActions && !isSelectionMode,
            fallbackIcon = fallbackIcon,
            fallbackLabel = fallbackLabel,
            colorSeedCode = colorSeedCode,
            onDismiss = { showQuickActions = false },
            onMessageClick = onMessageClick,
            onCallClick = onCallClick,
            onContactClick = onContactClick,
            onInfoClick = onInfoClick,
        )
    }
}

@Composable
private fun ConversationListAvatarQuickActions(
    item: ConversationListItemUiModel,
    visible: Boolean,
    fallbackIcon: ImageVector,
    fallbackLabel: String?,
    colorSeedCode: String?,
    onDismiss: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
    onInfoClick: () -> Unit,
) {
    ParticipantQuickActionsPopup(
        visible = visible,
        avatarUri = item.avatar.uri,
        displayName = item.title.orEmpty(),
        subtitle = item.avatar.details,
        fallbackIcon = fallbackIcon,
        fallbackLabel = fallbackLabel,
        colorSeedCode = colorSeedCode,
        onDismiss = onDismiss,
        onMessageClick = {
            onMessageClick()
            onDismiss()
        },
        onCallClick = {
            onCallClick?.invoke()
            onDismiss()
        }.takeIf { item.avatar.canCall && onCallClick != null },
        onContactClick = {
            onContactClick?.invoke()
            onDismiss()
        }.takeIf { item.avatar.canShowContact && onContactClick != null },
        onInfoClick = {
            onInfoClick()
            onDismiss()
        },
        isContactSaved = item.avatar.isContactSaved,
    )
}
