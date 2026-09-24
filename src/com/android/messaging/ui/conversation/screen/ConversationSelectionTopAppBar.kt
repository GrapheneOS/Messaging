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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.CONVERSATION_SELECTION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.conversationMessageSelectionActionButtonTestTag
import com.android.messaging.ui.conversation.screen.model.ConversationMessageAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

private val messageSelectionActions = persistentListOf(
    ConversationMessageAction.Download,
    ConversationMessageAction.Resend,
    ConversationMessageAction.Copy,
    ConversationMessageAction.Delete,
)

private val conversationMessageActions = persistentListOf(
    ConversationMessageAction.Share,
    ConversationMessageAction.Forward,
    ConversationMessageAction.SaveAttachment,
    ConversationMessageAction.Details,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationSelectionTopAppBar(
    selection: ConversationMessageSelectionUiState,
    onActionClick: (ConversationMessageAction) -> Unit,
    onDismissSelection: () -> Unit,
) {
    var isOverflowExpanded by remember {
        mutableStateOf(value = false)
    }

    val availableActions = selection.availableActions
    val overflowActions = remember(availableActions) {
        selectionActionsInOrder(
            availableActions = availableActions,
            orderedActions = conversationMessageActions,
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
        text = stringResource(
            id = R.string.conversation_message_selection_title,
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
    availableActions: ImmutableSet<ConversationMessageAction>,
    overflowActions: ImmutableList<ConversationMessageAction>,
    isOverflowExpanded: Boolean,
    onOverflowExpandedChange: (Boolean) -> Unit,
    onActionClick: (ConversationMessageAction) -> Unit,
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
    actions: ImmutableList<ConversationMessageAction>,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onActionClick: (ConversationMessageAction) -> Unit,
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
                    Text(text = stringResource(id = action.labelRes))
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
    action: ConversationMessageAction,
    onActionClick: (ConversationMessageAction) -> Unit,
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
            contentDescription = stringResource(id = action.labelRes),
        )
    }
}

private fun selectionActionsInOrder(
    availableActions: ImmutableSet<ConversationMessageAction>,
    orderedActions: ImmutableList<ConversationMessageAction>,
): ImmutableList<ConversationMessageAction> {
    return orderedActions.filter { action ->
        availableActions.contains(action)
    }.toPersistentList()
}

private fun selectionActionIcon(
    action: ConversationMessageAction,
): ImageVector {
    return when (action) {
        ConversationMessageAction.Copy -> Icons.Rounded.ContentCopy
        ConversationMessageAction.Delete -> Icons.Rounded.Delete
        ConversationMessageAction.Details -> Icons.Rounded.Info
        ConversationMessageAction.Download -> Icons.Rounded.FileDownload
        ConversationMessageAction.Forward -> Icons.AutoMirrored.Rounded.Forward
        ConversationMessageAction.Resend -> Icons.AutoMirrored.Rounded.Send
        ConversationMessageAction.SaveAttachment -> Icons.Rounded.Save
        ConversationMessageAction.Share -> Icons.Rounded.Share
    }
}

@PreviewLightDark
@Composable
private fun ConversationSelectionTopAppBarSingleMessagePreview() {
    MessagingPreviewTheme {
        ConversationSelectionTopAppBar(
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf(MessageId("message-1")),
                availableActions = persistentSetOf(
                    ConversationMessageAction.Copy,
                    ConversationMessageAction.Delete,
                    ConversationMessageAction.Forward,
                    ConversationMessageAction.Share,
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
                selectedMessageIds = persistentSetOf(
                    MessageId("message-1"),
                    MessageId("message-2"),
                    MessageId("message-3")
                ),
                availableActions = persistentSetOf(
                    ConversationMessageAction.Delete,
                    ConversationMessageAction.SaveAttachment,
                    ConversationMessageAction.Details,
                    ConversationMessageAction.Resend,
                    ConversationMessageAction.Download,
                ),
            ),
            onActionClick = { _ -> },
            onDismissSelection = {},
        )
    }
}
