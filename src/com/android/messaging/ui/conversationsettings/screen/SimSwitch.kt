package com.android.messaging.ui.conversationsettings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.ui.common.text.asLtrText
import com.android.messaging.ui.conversationsettings.common.settingsCardShape
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction as Action
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState as State
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.subscription.component.SubscriptionSimAvatar
import com.android.messaging.ui.subscription.mapper.resolveDisplayName
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal fun LazyListScope.simSwitchItem(
    uiState: State,
    onAction: (Action) -> Unit,
) {
    if (!uiState.isSimSwitchAvailable) return

    val selected = uiState.selectedSubscription ?: return

    item(key = "sim_switch") {
        SimSwitchItem(
            subscriptions = uiState.availableSubscriptions,
            selected = selected,
            onSimSelected = { selfParticipantId ->
                onAction(Action.SimSelected(selfParticipantId))
            },
        )
    }
}

@Composable
private fun SimSwitchItem(
    subscriptions: ImmutableList<Subscription>,
    selected: Subscription,
    onSimSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.settingsCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = { expanded = true },
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 8.dp,
                top = 14.dp,
                bottom = 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        ) {
            Text(
                text = stringResource(R.string.sim_selector_item_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.SimCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.width(20.dp))

                SimSwitchItemSummary(
                    selected = selected,
                    modifier = Modifier.weight(1f),
                )

                SimSwitchItemMenu(
                    subscriptions = subscriptions,
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    onSimSelected = { id ->
                        expanded = false
                        onSimSelected(id)
                    },
                )
            }
        }
    }
}

@Composable
private fun SimSwitchItemSummary(
    selected: Subscription,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = selected.label.resolveDisplayName(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        selected.displayDestination?.let { destination ->
            Text(
                text = destination.asLtrText(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SimSwitchItemMenu(
    subscriptions: ImmutableList<Subscription>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSimSelected: (String) -> Unit,
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = stringResource(R.string.sim_selector_item_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(
            expanded = expanded,
            shape = MaterialTheme.settingsCardShape,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            SimSelectorPopupContent(
                subscriptions = subscriptions,
                onSimSelected = onSimSelected,
            )
        }
    }
}

@Composable
private fun SimSelectorPopupContent(
    subscriptions: ImmutableList<Subscription>,
    onSimSelected: (String) -> Unit,
) {
    Column {
        subscriptions.forEach { subscription ->
            SimSelectorRow(
                subscription = subscription,
                onClick = { onSimSelected(subscription.selfParticipantId) },
            )
        }
    }
}

@Composable
private fun SimSelectorRow(
    subscription: Subscription,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        SubscriptionSimAvatar(
            subscription = subscription,
            size = 24.dp,
        )

        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = subscription.label.resolveDisplayName(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            subscription.displayDestination?.let { destination ->
                val displayDestination = destination.asLtrText()

                Text(
                    text = displayDestination,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SimSwitchItemPreview() {
    val subscription = Subscription(
        selfParticipantId = "1",
        subId = 1,
        label = ConversationSubscriptionLabel.Slot(slotId = 1),
        displayDestination = "+31 6 1234 5678",
        displaySlotId = 1,
        color = 0,
    )
    MessagingPreviewColumn {
        SimSwitchItem(
            subscriptions = persistentListOf(subscription),
            selected = subscription,
            onSimSelected = {},
        )
    }
}
