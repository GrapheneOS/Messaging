package com.android.messaging.ui.conversationlist.archived

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListAction as Action
import com.android.messaging.ui.core.MessagingPreviewTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArchivedConversationListSelectionTopAppBar(
    selectedCount: Int,
    onAction: (Action) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        colors = selectionTopAppBarColors(),
        title = {
            Text(
                text = stringResource(
                    R.string.conversation_message_selection_title,
                    selectedCount,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            SelectionActionButton(
                imageVector = Icons.Filled.Unarchive,
                labelResId = R.string.action_unarchive,
                onClick = { onAction(Action.UnarchiveSelectedClicked) },
            )

            SelectionActionButton(
                imageVector = Icons.Filled.Delete,
                labelResId = R.string.action_delete,
                onClick = onDeleteClick,
            )
        },
    )
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
private fun ArchivedConversationListSelectionTopAppBarPreview() {
    MessagingPreviewTheme {
        ArchivedConversationListSelectionTopAppBar(
            selectedCount = 2,
            onAction = {},
            onDeleteClick = {},
        )
    }
}
