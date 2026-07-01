package com.android.messaging.ui.conversationlist.chats

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.chats.model.SelectionActionsUiState
import com.android.messaging.ui.core.MessagingPreviewTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationListSelectionTopAppBar(
    selectedCount: Int,
    actions: SelectionActionsUiState,
    onAction: (Action) -> Unit,
    onDeleteClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        colors = selectionTopAppBarColors(),
        title = {
            ConversationListSelectionTitle(selectedCount)
        },
        navigationIcon = {
            IconButton(onClick = { onAction(Action.SelectionCleared) }) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close_selection),
                )
            }
        },
        actions = {
            ConversationListSelectionActions(
                actions = actions,
                onAction = onAction,
                onDeleteClick = onDeleteClick,
                onSnoozeClick = onSnoozeClick,
            )
        },
    )
}

@Composable
private fun ConversationListSelectionTitle(selectedCount: Int) {
    Text(
        text = stringResource(
            R.string.conversation_message_selection_title,
            selectedCount,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ConversationListSelectionActions(
    actions: SelectionActionsUiState,
    onAction: (Action) -> Unit,
    onDeleteClick: () -> Unit,
    onSnoozeClick: () -> Unit,
) {
    actions.firstSelectedIsSnoozed?.let { isSnoozed ->
        when {
            isSnoozed -> SelectionActionButton(
                imageVector = Icons.Default.NotificationsActive,
                labelResId = R.string.unsnooze_chat_setting_title,
                onClick = { onAction(Action.UnsnoozeClicked) },
            )

            else -> SelectionActionButton(
                imageVector = Icons.Default.Snooze,
                labelResId = R.string.snooze_chat_setting_title,
                onClick = onSnoozeClick,
            )
        }
    }

    actions.firstSelectedIsPinned?.let { isPinned ->
        when {
            isPinned -> SelectionActionButton(
                imageVector = Icons.Outlined.PushPin,
                labelResId = R.string.action_unpin,
                onClick = { onAction(Action.UnpinClicked) },
            )

            else -> SelectionActionButton(
                imageVector = Icons.Filled.PushPin,
                labelResId = R.string.action_pin,
                onClick = { onAction(Action.PinClicked) },
            )
        }
    }

    SelectionActionButton(
        imageVector = Icons.Default.Archive,
        labelResId = R.string.action_archive,
        onClick = { onAction(Action.ArchiveClicked) },
    )

    SelectionActionButton(
        imageVector = Icons.Default.Delete,
        labelResId = R.string.action_delete,
        onClick = onDeleteClick,
    )

    SelectionOverflowMenu(
        actions = actions,
        onAction = onAction,
    )
}

@Composable
private fun SelectionOverflowMenu(
    actions: SelectionActionsUiState,
    onAction: (Action) -> Unit,
) {
    OverflowMenu { dismiss ->
        actions.firstSelectedIsUnread?.let { isUnread ->
            OverflowMenuItem(
                labelResId = when {
                    isUnread -> R.string.mark_as_read
                    else -> R.string.mark_as_unread
                },
                onClick = {
                    val action = when {
                        isUnread -> Action.MarkReadClicked
                        else -> Action.MarkUnreadClicked
                    }
                    onAction(action)
                    dismiss()
                },
            )
        }

        if (actions.canAddContact) {
            OverflowMenuItem(
                labelResId = R.string.action_add_contact,
                onClick = {
                    onAction(Action.AddContactClicked)
                    dismiss()
                },
            )
        }

        if (actions.canBlock) {
            OverflowMenuItem(
                labelResId = R.string.action_block,
                onClick = {
                    onAction(Action.BlockClicked)
                    dismiss()
                },
            )
        }
    }
}

@Composable
private fun SelectionActionButton(
    imageVector: ImageVector,
    labelResId: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = imageVector,
            contentDescription = stringResource(labelResId),
        )
    }
}

@Composable
private fun selectionTopAppBarColors(): TopAppBarColors {
    return TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@PreviewLightDark
@Composable
private fun ConversationListSelectionTopAppBarPreview() {
    MessagingPreviewTheme {
        ConversationListSelectionTopAppBar(
            selectedCount = 2,
            actions = SelectionActionsUiState(
                canAddContact = true,
                canBlock = true,
                firstSelectedIsPinned = false,
                firstSelectedIsSnoozed = false,
                firstSelectedIsUnread = true,
            ),
            onAction = {},
            onDeleteClick = {},
            onSnoozeClick = {},
        )
    }
}
