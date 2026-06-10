package com.android.messaging.ui.conversationlist.redesign.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.core.MessagingPreviewTheme

@Composable
internal fun ConversationListAddContactDialog(
    destination: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.add_contact_confirmation_dialog_title))
        },
        text = {
            Text(text = destination)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.add_contact_confirmation))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
internal fun ConversationListDeleteDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = pluralStringResource(
                    id = R.plurals.delete_conversations_confirmation_dialog_title,
                    count = selectedCount,
                ),
            )
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

@Composable
internal fun ConversationListBlockDialog(
    destination: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.block_confirmation_title, destination))
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
private fun ConversationListAddContactDialogPreview() {
    MessagingPreviewTheme {
        ConversationListAddContactDialog(
            destination = "+1 555 0100",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationListDeleteDialogPreview() {
    MessagingPreviewTheme {
        ConversationListDeleteDialog(
            selectedCount = 2,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationListBlockDialogPreview() {
    MessagingPreviewTheme {
        ConversationListBlockDialog(
            destination = "+1 555 0100",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
