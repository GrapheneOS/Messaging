package com.android.messaging.ui.conversationlist.chats

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.common.components.SnoozeChatDialog
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.common.dialog.ConversationListDeleteDialog
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ConversationListDialogs(
    selectedCount: Int,
    deleteConversationIds: ImmutableList<ConversationId>?,
    blockConversationId: ConversationId?,
    blockDestination: String?,
    isSnoozeVisible: Boolean,
    onAction: (Action) -> Unit,
    onDismissDelete: () -> Unit,
    onDismissBlock: () -> Unit,
    onDismissSnooze: () -> Unit,
) {
    val hasSelectedConversations = selectedCount > 0

    DismissSelectionDialogsWithoutSelection(
        selectedCount = selectedCount,
        isSnoozeVisible = isSnoozeVisible,
        onDismissSnooze = onDismissSnooze,
    )

    if (deleteConversationIds != null) {
        ConversationListDeleteDialog(
            selectedCount = deleteConversationIds.size,
            onConfirm = {
                onAction(Action.DeleteConfirmed(deleteConversationIds))
                onDismissDelete()
            },
            onDismiss = onDismissDelete,
        )
    }

    if (blockConversationId != null && blockDestination != null) {
        ConversationListBlockDialog(
            destination = blockDestination,
            onConfirm = {
                onAction(
                    Action.BlockConfirmed(
                        conversationId = blockConversationId,
                        destination = blockDestination,
                    ),
                )
                onDismissBlock()
            },
            onDismiss = onDismissBlock,
        )
    }

    if (isSnoozeVisible && hasSelectedConversations) {
        SnoozeChatDialog(
            count = selectedCount,
            onDismiss = onDismissSnooze,
            onConfirm = { option ->
                onAction(Action.SnoozeOptionSelected(option))
                onDismissSnooze()
            },
        )
    }
}

@Composable
private fun DismissSelectionDialogsWithoutSelection(
    selectedCount: Int,
    isSnoozeVisible: Boolean,
    onDismissSnooze: () -> Unit,
) {
    LaunchedEffect(
        selectedCount,
        isSnoozeVisible,
    ) {
        if (selectedCount > 0) {
            return@LaunchedEffect
        }

        if (isSnoozeVisible) {
            onDismissSnooze()
        }
    }
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
private fun ConversationListBlockDialogPreview() {
    MessagingPreviewTheme {
        ConversationListBlockDialog(
            destination = "+1 555 0100",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
