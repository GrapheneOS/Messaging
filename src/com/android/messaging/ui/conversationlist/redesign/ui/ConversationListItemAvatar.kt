package com.android.messaging.ui.conversationlist.redesign.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.android.messaging.ui.common.components.participant.ParticipantAvatar
import com.android.messaging.ui.common.components.participant.participantAvatarLabel
import com.android.messaging.ui.common.components.participant.participantColorSeed
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListItemUiModel

@Composable
internal fun ConversationListItemAvatar(
    item: ConversationListItemUiModel,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
) {
    val fallbackIcon = when {
        item.avatar.isGroup -> Icons.Default.Group
        else -> Icons.Default.Person
    }

    Box(modifier = Modifier.size(ItemAvatarSize)) {
        ParticipantAvatar(
            avatarUri = item.avatar.uri,
            size = ItemAvatarSize,
            fallbackLabel = participantAvatarLabel(source = item.title),
            colorSeedCode = participantColorSeed(
                normalizedDestination = item.avatar.normalizedDestination,
            ),
            fallbackSize = ItemAvatarFallbackSize,
            fallbackIcon = fallbackIcon,
            isSelected = item.isSelected,
            modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    if (isSelectionMode) {
                        onToggleSelection()
                    }
                },
        )
    }
}
