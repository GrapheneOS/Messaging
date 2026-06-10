package com.android.messaging.ui.conversationlist.redesign.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Unarchive
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.redesign.model.SelectionActionsUiState
import com.android.messaging.ui.core.MessagingPreviewTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationListSelectionTopAppBar(
    selectedCount: Int,
    actions: SelectionActionsUiState,
    onAction: (Action) -> Unit,
    onDeleteClick: () -> Unit,
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
            )
        },
    )
}

@Composable
private fun ConversationListSelectionTitle(selectedCount: Int) {
    Text(
        text = pluralStringResource(
            id = R.plurals.conversation_message_selection_title,
            count = selectedCount,
            selectedCount,
        ),
    )
}

@Composable
private fun ConversationListSelectionActions(
    actions: SelectionActionsUiState,
    onAction: (Action) -> Unit,
    onDeleteClick: () -> Unit,
) {
    if (actions.canArchive) {
        SelectionActionButton(
            imageVector = Icons.Default.Archive,
            labelResId = R.string.action_archive,
            onClick = { onAction(Action.ArchiveClicked) },
        )
    }

    if (actions.canUnarchive) {
        SelectionActionButton(
            imageVector = Icons.Default.Unarchive,
            labelResId = R.string.action_unarchive,
            onClick = { onAction(Action.UnarchiveClicked) },
        )
    }

    if (actions.canAddContact) {
        SelectionActionButton(
            imageVector = Icons.Default.PersonAdd,
            labelResId = R.string.action_add_contact,
            onClick = { onAction(Action.AddContactClicked) },
        )
    }

    if (actions.canBlock) {
        SelectionActionButton(
            imageVector = Icons.Default.Block,
            labelResId = R.string.action_block,
            onClick = { onAction(Action.BlockClicked) },
        )
    }

    if (actions.canDelete) {
        SelectionActionButton(
            imageVector = Icons.Default.Delete,
            labelResId = R.string.action_delete,
            onClick = onDeleteClick,
        )
    }
}

@Composable
private fun SelectionActionButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
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
                canArchive = true,
                canDelete = true,
                canAddContact = true,
                canBlock = true,
            ),
            onAction = {},
            onDeleteClick = {},
        )
    }
}
