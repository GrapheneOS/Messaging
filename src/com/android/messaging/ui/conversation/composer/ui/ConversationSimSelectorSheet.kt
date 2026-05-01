package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.conversation.model.metadata.ConversationSubscription
import com.android.messaging.ui.conversation.CONVERSATION_SIM_SELECTOR_SHEET_TEST_TAG
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.conversationSimSelectorItemTestTag
import com.android.messaging.ui.conversation.resolveDisplayName

private val SHEET_VERTICAL_PADDING = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationSimSelectorSheet(
    uiState: ConversationSimSelectorUiState,
    onSimSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    ModalBottomSheet(
        modifier = Modifier.testTag(tag = CONVERSATION_SIM_SELECTOR_SHEET_TEST_TAG),
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        ConversationSimSelectorSheetContent(
            uiState = uiState,
            onSimSelected = onSimSelected,
        )
    }
}

@Composable
private fun ConversationSimSelectorSheetContent(
    uiState: ConversationSimSelectorUiState,
    onSimSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = SHEET_VERTICAL_PADDING),
    ) {
        Text(
            modifier = Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                top = 8.dp,
                bottom = 12.dp,
            ),
            text = stringResource(id = R.string.sim_selector_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        uiState.subscriptions.forEach { subscription ->
            val isSelected = subscription.selfParticipantId ==
                uiState.selectedSubscription?.selfParticipantId

            ConversationSimSelectorRow(
                subscription = subscription,
                isSelected = isSelected,
                onClick = { onSimSelected(subscription.selfParticipantId) },
            )
        }

        Spacer(modifier = Modifier.height(height = SHEET_VERTICAL_PADDING))
    }
}

@Composable
private fun ConversationSimSelectorRow(
    subscription: ConversationSubscription,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag(
                tag = conversationSimSelectorItemTestTag(
                    selfParticipantId = subscription.selfParticipantId,
                ),
            )
            .padding(
                horizontal = 24.dp,
                vertical = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        ConversationSimAvatar(subscription = subscription)

        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = subscription.label.resolveDisplayName(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            subscription.displayDestination?.let { destination ->
                Text(
                    text = destination,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(id = R.string.sim_selector_item_selected),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ConversationSimAvatar(
    subscription: ConversationSubscription,
) {
    Box(
        modifier = Modifier
            .size(size = 40.dp)
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
private fun ConversationSubscription.resolveAccentColor(): Color {
    return when (color) {
        0 -> MaterialTheme.colorScheme.primary
        else -> Color(color = color)
    }
}
