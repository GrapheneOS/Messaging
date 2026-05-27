package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.ui.conversation.preview.previewSubscriptions
import com.android.messaging.ui.core.MessagingPreviewColumn

internal val ConversationSimAvatarDefaultSize: Dp = 40.dp

@Composable
internal fun ConversationSimAvatar(
    subscription: Subscription,
    modifier: Modifier = Modifier,
    size: Dp = ConversationSimAvatarDefaultSize,
) {
    Box(
        modifier = modifier
            .size(size = size)
            .clip(shape = CircleShape)
            .background(
                color = subscription.resolveAccentColor(),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = subscription.displaySlotId.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun Subscription.resolveAccentColor(): Color {
    return when (color) {
        0 -> MaterialTheme.colorScheme.primary
        else -> Color(color = color)
    }
}

@PreviewLightDark
@Composable
private fun ConversationSimAvatarPreview() {
    MessagingPreviewColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            previewSubscriptions().forEach { subscription ->
                ConversationSimAvatar(subscription = subscription)
            }
        }
    }
}
