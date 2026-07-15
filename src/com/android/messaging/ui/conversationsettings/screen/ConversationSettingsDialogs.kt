package com.android.messaging.ui.conversationsettings.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.common.components.SnoozeChatDialog
import com.android.messaging.ui.common.text.asLtrText
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction as Action
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState as State
import com.android.messaging.ui.core.MessagingPreviewTheme

@Composable
internal fun ConversationSettingsDialogs(
    uiState: State,
    onAction: (Action) -> Unit,
    pendingBlockConfirmation: Boolean,
    showSnoozeChatDialog: Boolean,
    onDismissBlockConfirmation: () -> Unit,
    onDismissSnoozeChat: () -> Unit,
) {
    if (pendingBlockConfirmation) {
        val displayName = uiState.otherParticipant?.displayDestination.orEmpty().asLtrText()

        BlockConfirmationDialog(
            displayName = displayName,
            onDismiss = onDismissBlockConfirmation,
            onConfirm = {
                onAction(Action.BlockConfirmed)
                onDismissBlockConfirmation()
            },
        )
    }

    if (showSnoozeChatDialog) {
        SnoozeChatDialog(
            count = 1,
            onDismiss = onDismissSnoozeChat,
            onConfirm = { option ->
                onAction(Action.SnoozeOptionSelected(option))
                onDismissSnoozeChat()
            },
        )
    }
}

@Composable
private fun BlockConfirmationDialog(
    displayName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.block_confirmation_title, displayName))
        },
        text = {
            Text(text = stringResource(R.string.block_confirmation_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun BlockConfirmationDialogPreview() {
    MessagingPreviewTheme {
        BlockConfirmationDialog(
            displayName = "+31 6 1234 5678",
            onDismiss = {},
            onConfirm = {},
        )
    }
}
