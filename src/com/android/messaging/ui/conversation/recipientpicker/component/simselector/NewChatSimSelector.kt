package com.android.messaging.ui.conversation.recipientpicker.component.simselector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.ui.conversation.NEW_CHAT_SIM_SELECTOR_CHIP_TEST_TAG
import com.android.messaging.ui.conversation.NEW_CHAT_SIM_SELECTOR_DROPDOWN_TEST_TAG
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.composer.ui.ConversationSimAvatar
import com.android.messaging.ui.conversation.newChatSimSelectorItemTestTag
import com.android.messaging.ui.conversation.preview.previewSimSelectorUiState
import com.android.messaging.ui.conversation.resolveDisplayName
import com.android.messaging.ui.core.MessagingPreviewColumn
import kotlinx.collections.immutable.ImmutableList

private val ChipAvatarSize = 24.dp
private val DropdownAvatarSize = 32.dp

@Composable
internal fun NewChatSimSelectorRow(
    uiState: ConversationSimSelectorUiState,
    onSimSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!uiState.isAvailable) {
        return
    }

    val selectedSubscription = uiState.selectedSubscription ?: return

    var isDropdownExpanded by rememberSaveable { mutableStateOf(value = false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.new_chat_sim_selector_prefix),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box {
            NewChatSimSelectorChip(
                subscription = selectedSubscription,
                onClick = { isDropdownExpanded = true },
            )

            NewChatSimSelectorDropdown(
                expanded = isDropdownExpanded,
                subscriptions = uiState.subscriptions,
                selectedSubscription = selectedSubscription,
                onSimSelected = onSimSelected,
                onDismissRequest = { isDropdownExpanded = false },
            )
        }
    }
}

@Composable
private fun NewChatSimSelectorChip(
    subscription: Subscription,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = subscription.label.resolveDisplayName()
    val chipDescription = stringResource(
        id = R.string.new_chat_sim_selector_chip_content_description,
        label,
    )

    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.small)
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag = NEW_CHAT_SIM_SELECTOR_CHIP_TEST_TAG)
            .semantics { contentDescription = chipDescription }
            .padding(
                start = 6.dp,
                end = 8.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        ConversationSimAvatar(
            subscription = subscription,
            size = ChipAvatarSize,
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Icon(
            imageVector = Icons.Rounded.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NewChatSimSelectorDropdown(
    expanded: Boolean,
    subscriptions: ImmutableList<Subscription>,
    selectedSubscription: Subscription?,
    onSimSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        modifier = modifier
            .testTag(tag = NEW_CHAT_SIM_SELECTOR_DROPDOWN_TEST_TAG),
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        subscriptions.forEach { subscription ->
            val isSelected = subscription.selfParticipantId ==
                selectedSubscription?.selfParticipantId

            NewChatSimSelectorDropdownItem(
                subscription = subscription,
                isSelected = isSelected,
                onClick = {
                    onSimSelected(subscription.selfParticipantId)
                    onDismissRequest()
                },
            )
        }
    }
}

@Composable
private fun NewChatSimSelectorDropdownItem(
    subscription: Subscription,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label = subscription.label.resolveDisplayName()

    DropdownMenuItem(
        modifier = Modifier
            .testTag(
                tag = newChatSimSelectorItemTestTag(
                    selfParticipantId = subscription.selfParticipantId,
                ),
            ),
        onClick = onClick,
        leadingIcon = {
            ConversationSimAvatar(
                subscription = subscription,
                size = DropdownAvatarSize,
            )
        },
        text = {
            NewChatSimSelectorDropdownItemText(
                label = label,
                destination = subscription.displayDestination,
            )
        },
        trailingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(id = R.string.sim_selector_item_selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

@Composable
private fun NewChatSimSelectorDropdownItemText(
    label: String,
    destination: String?,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        destination?.let {
            Text(
                text = destination,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NewChatSimSelectorRowPreview() {
    MessagingPreviewColumn {
        NewChatSimSelectorRow(
            uiState = previewSimSelectorUiState(),
            onSimSelected = { _ -> },
        )
    }
}
