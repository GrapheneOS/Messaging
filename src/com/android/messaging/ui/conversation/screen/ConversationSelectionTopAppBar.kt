package com.android.messaging.ui.conversation.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_SELECTION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.conversationMessageSelectionActionButtonTestTag
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

private val messageSelectionActions = persistentListOf(
    ConversationMessageSelectionAction.Download,
    ConversationMessageSelectionAction.Resend,
    ConversationMessageSelectionAction.Copy,
    ConversationMessageSelectionAction.Delete,
)

private val conversationMessageSelectionActions = persistentListOf(
    ConversationMessageSelectionAction.Share,
    ConversationMessageSelectionAction.Forward,
    ConversationMessageSelectionAction.SaveAttachment,
    ConversationMessageSelectionAction.Details,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationSelectionTopAppBar(
    selection: ConversationMessageSelectionUiState,
    onActionClick: (ConversationMessageSelectionAction) -> Unit,
    onDismissSelection: () -> Unit,
) {
    var isOverflowExpanded by remember {
        mutableStateOf(value = false)
    }

    val availableActions = selection.availableActions
    val overflowActions = remember(availableActions) {
        selectionActionsInOrder(
            availableActions = availableActions,
            orderedActions = conversationMessageSelectionActions,
        )
    }

    TopAppBar(
        colors = conversationSelectionTopAppBarColors(),
        title = {
            ConversationSelectionTitle(selectedMessageCount = selection.selectedMessageCount)
        },
        navigationIcon = {
            ConversationSelectionNavigationIcon(onDismissSelection = onDismissSelection)
        },
        actions = {
            ConversationSelectionActions(
                availableActions = availableActions,
                overflowActions = overflowActions,
                isOverflowExpanded = isOverflowExpanded,
                onOverflowExpandedChange = { isExpanded ->
                    isOverflowExpanded = isExpanded
                },
                onActionClick = onActionClick,
            )
        },
    )
}

@Composable
private fun ConversationSelectionTitle(selectedMessageCount: Int) {
    Text(
        text = pluralStringResource(
            id = R.plurals.conversation_message_selection_title,
            count = selectedMessageCount,
            selectedMessageCount,
        ),
    )
}

@Composable
private fun ConversationSelectionNavigationIcon(onDismissSelection: () -> Unit) {
    IconButton(
        onClick = onDismissSelection,
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(
                id = R.string.close_selection,
            ),
        )
    }
}

@Composable
private fun ConversationSelectionActions(
    availableActions: ImmutableSet<ConversationMessageSelectionAction>,
    overflowActions: ImmutableList<ConversationMessageSelectionAction>,
    isOverflowExpanded: Boolean,
    onOverflowExpandedChange: (Boolean) -> Unit,
    onActionClick: (ConversationMessageSelectionAction) -> Unit,
) {
    val primaryActions = remember(availableActions) {
        selectionActionsInOrder(
            availableActions = availableActions,
            orderedActions = messageSelectionActions,
        )
    }

    primaryActions.forEach { action ->
        ConversationSelectionActionButton(
            action = action,
            onActionClick = onActionClick,
        )
    }

    if (overflowActions.isNotEmpty()) {
        ConversationSelectionOverflowButton(
            onClick = {
                onOverflowExpandedChange(true)
            },
        )
        ConversationSelectionOverflowMenu(
            actions = overflowActions,
            expanded = isOverflowExpanded,
            onDismissRequest = {
                onOverflowExpandedChange(false)
            },
            onActionClick = onActionClick,
        )
    }
}

@Composable
private fun ConversationSelectionOverflowButton(onClick: () -> Unit) {
    IconButton(
        modifier = Modifier
            .testTag(
                tag = CONVERSATION_SELECTION_OVERFLOW_BUTTON_TEST_TAG,
            ),
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(
                id = R.string.more_options,
            ),
        )
    }
}

@Composable
private fun ConversationSelectionOverflowMenu(
    actions: ImmutableList<ConversationMessageSelectionAction>,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onActionClick: (ConversationMessageSelectionAction) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        actions.forEach { action ->
            DropdownMenuItem(
                modifier = Modifier.testTag(
                    tag = conversationMessageSelectionActionButtonTestTag(
                        action = action.name,
                    ),
                ),
                text = {
                    Text(text = selectionActionLabel(action = action))
                },
                onClick = {
                    onDismissRequest()
                    onActionClick(action)
                },
                leadingIcon = {
                    Icon(
                        imageVector = selectionActionIcon(action = action),
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@Composable
private fun conversationSelectionTopAppBarColors(): TopAppBarColors {
    return TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
private fun ConversationSelectionActionButton(
    action: ConversationMessageSelectionAction,
    onActionClick: (ConversationMessageSelectionAction) -> Unit,
) {
    IconButton(
        modifier = Modifier.testTag(
            tag = conversationMessageSelectionActionButtonTestTag(action = action.name),
        ),
        onClick = {
            onActionClick(action)
        },
    ) {
        Icon(
            imageVector = selectionActionIcon(action = action),
            contentDescription = selectionActionLabel(action = action),
        )
    }
}

private fun selectionActionsInOrder(
    availableActions: ImmutableSet<ConversationMessageSelectionAction>,
    orderedActions: ImmutableList<ConversationMessageSelectionAction>,
): ImmutableList<ConversationMessageSelectionAction> {
    return orderedActions.filter { action ->
        availableActions.contains(action)
    }.toPersistentList()
}

private fun selectionActionIcon(
    action: ConversationMessageSelectionAction,
): ImageVector {
    return when (action) {
        ConversationMessageSelectionAction.Copy -> Icons.Rounded.ContentCopy
        ConversationMessageSelectionAction.Delete -> Icons.Rounded.Delete
        ConversationMessageSelectionAction.Details -> Icons.Rounded.Info
        ConversationMessageSelectionAction.Download -> Icons.Rounded.FileDownload
        ConversationMessageSelectionAction.Forward -> Icons.AutoMirrored.Rounded.Forward
        ConversationMessageSelectionAction.Resend -> Icons.AutoMirrored.Rounded.Send
        ConversationMessageSelectionAction.SaveAttachment -> Icons.Rounded.Save
        ConversationMessageSelectionAction.Share -> Icons.Rounded.Share
    }
}

@Composable
private fun selectionActionLabel(
    action: ConversationMessageSelectionAction,
): String {
    return when (action) {
        ConversationMessageSelectionAction.Copy -> {
            stringResource(R.string.message_context_menu_copy_text)
        }
        ConversationMessageSelectionAction.Delete -> {
            stringResource(R.string.action_delete_message)
        }
        ConversationMessageSelectionAction.Details -> {
            stringResource(R.string.message_context_menu_view_details)
        }
        ConversationMessageSelectionAction.Download -> {
            stringResource(R.string.action_download)
        }
        ConversationMessageSelectionAction.Forward -> {
            stringResource(R.string.message_context_menu_forward_message)
        }
        ConversationMessageSelectionAction.Resend -> {
            stringResource(R.string.action_send)
        }
        ConversationMessageSelectionAction.SaveAttachment -> {
            stringResource(R.string.action_save_attachment)
        }
        ConversationMessageSelectionAction.Share -> {
            stringResource(R.string.action_share)
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationSelectionTopAppBarSingleMessagePreview() {
    MessagingPreviewTheme {
        ConversationSelectionTopAppBar(
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-1"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Copy,
                    ConversationMessageSelectionAction.Delete,
                    ConversationMessageSelectionAction.Forward,
                    ConversationMessageSelectionAction.Share,
                ),
            ),
            onActionClick = { _ -> },
            onDismissSelection = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationSelectionTopAppBarMultiMessagePreview() {
    MessagingPreviewTheme {
        ConversationSelectionTopAppBar(
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-1", "message-2", "message-3"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Delete,
                    ConversationMessageSelectionAction.SaveAttachment,
                    ConversationMessageSelectionAction.Details,
                    ConversationMessageSelectionAction.Resend,
                    ConversationMessageSelectionAction.Download,
                ),
            ),
            onActionClick = { _ -> },
            onDismissSelection = {},
        )
    }
}
