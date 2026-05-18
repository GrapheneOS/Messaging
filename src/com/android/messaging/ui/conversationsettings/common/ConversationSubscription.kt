package com.android.messaging.ui.conversationsettings.common

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription

internal val ConversationSimAvatarDefaultSize: Dp = 24.dp

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
            .background(color = subscription.resolveAccentColor()),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = density.density,
                fontScale = 1f,
            ),
        ) {
            Text(
                text = subscription.displaySlotId.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
internal fun ConversationSubscriptionLabel.resolveDisplayName(): String {
    return resolveDisplayName(resources = LocalResources.current)
}

internal fun ConversationSubscriptionLabel.resolveDisplayName(
    resources: Resources,
): String {
    return when (this) {
        is ConversationSubscriptionLabel.Named -> name

        is ConversationSubscriptionLabel.Slot -> {
            resources.getString(R.string.sim_slot_identifier, slotId.toString())
        }

        is ConversationSubscriptionLabel.DebugFake -> {
            resources.getString(R.string.debug_emulated_sim_display_name, slotId.toString())
        }
    }
}

@Composable
private fun Subscription.resolveAccentColor(): Color {
    return when (color) {
        0 -> MaterialTheme.colorScheme.primary
        else -> Color(color = color)
    }
}
