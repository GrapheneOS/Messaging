package com.android.messaging.ui.blockedparticipants.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.messaging.R
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsAction as Action
import com.android.messaging.ui.core.AppTheme

@Composable
internal fun BlockedParticipantsDialogs(
    selectedCount: Int,
    onAction: (Action) -> Unit,
    showDeleteConfirmation: Boolean,
    onDismissDeleteConfirmation: () -> Unit,
) {
    if (showDeleteConfirmation) {
        DeleteSelectedConfirmationDialog(
            selectedCount = selectedCount,
            onConfirm = {
                onAction(Action.DeleteSelectedConfirmed)
                onDismissDeleteConfirmation()
            },
            onDismiss = onDismissDeleteConfirmation,
        )
    }
}

@Composable
private fun DeleteSelectedConfirmationDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = pluralStringResource(
                    R.plurals.delete_conversations_confirmation_dialog_title,
                    selectedCount,
                ),
            )
        },
        text = {
            Text(text = stringResource(R.string.delete_message_confirmation_dialog_text))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.delete_conversation_confirmation_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.delete_conversation_decline_button))
            }
        },
    )
}

@Preview
@Composable
private fun DeleteSelectedConfirmationDialogSinglePreview() {
    AppTheme {
        DeleteSelectedConfirmationDialog(
            selectedCount = 1,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun DeleteSelectedConfirmationDialogMultiplePreview() {
    AppTheme {
        DeleteSelectedConfirmationDialog(
            selectedCount = 3,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
