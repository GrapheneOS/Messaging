package com.android.messaging.ui.subscription.mapper

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import com.android.messaging.R
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.ui.subscription.model.SimOptionUiModel
import com.android.messaging.ui.subscription.model.SimSelectorUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

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
internal fun rememberSimSelectorUiState(
    subscriptions: ImmutableList<Subscription>,
    selectedSelfParticipantId: String?,
): SimSelectorUiState {
    val resources = LocalResources.current

    return remember(subscriptions, selectedSelfParticipantId, resources) {
        SimSelectorUiState(
            options = subscriptions
                .map { subscription -> subscription.toSimOptionUiModel(resources = resources) }
                .toImmutableList(),
            selectedId = selectedSelfParticipantId,
        )
    }
}

internal fun Subscription.toSimOptionUiModel(resources: Resources): SimOptionUiModel {
    return SimOptionUiModel(
        id = selfParticipantId,
        label = label.resolveDisplayName(resources = resources),
        destination = displayDestination,
        slotLabel = displaySlotId.toString(),
        accentColor = toSimAccentColor(),
    )
}

internal fun Subscription.toSimAccentColor(): Color? {
    return when (color) {
        0 -> null
        else -> Color(color = color)
    }
}
