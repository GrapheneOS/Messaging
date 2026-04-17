package com.android.messaging.ui.conversation.v2.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionUiState

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

    val overflowActions = remember(selection.availableActions) {
        buildList {
            if (selection.availableActions.contains(ConversationMessageSelectionAction.Share)) {
                add(ConversationMessageSelectionAction.Share)
            }

            if (selection.availableActions.contains(ConversationMessageSelectionAction.Forward)) {
                add(ConversationMessageSelectionAction.Forward)
            }

            if (selection.availableActions.contains(ConversationMessageSelectionAction.Details)) {
                add(ConversationMessageSelectionAction.Details)
            }
        }
    }

    TopAppBar(
        colors = conversationSelectionTopAppBarColors(),
        title = {
            Text(
                text = pluralStringResource(
                    id = R.plurals.conversation_message_selection_title,
                    count = selection.selectedMessageCount,
                    selection.selectedMessageCount,
                ),
            )
        },
        navigationIcon = {
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
        },
        actions = {
            if (selection.availableActions.contains(ConversationMessageSelectionAction.Download)) {
                ConversationSelectionActionButton(
                    action = ConversationMessageSelectionAction.Download,
                    onActionClick = onActionClick,
                )
            }

            if (selection.availableActions.contains(ConversationMessageSelectionAction.Resend)) {
                ConversationSelectionActionButton(
                    action = ConversationMessageSelectionAction.Resend,
                    onActionClick = onActionClick,
                )
            }

            if (selection.availableActions.contains(ConversationMessageSelectionAction.Copy)) {
                ConversationSelectionActionButton(
                    action = ConversationMessageSelectionAction.Copy,
                    onActionClick = onActionClick,
                )
            }

            if (selection.availableActions.contains(ConversationMessageSelectionAction.Delete)) {
                ConversationSelectionActionButton(
                    action = ConversationMessageSelectionAction.Delete,
                    onActionClick = onActionClick,
                )
            }

            if (overflowActions.isNotEmpty()) {
                IconButton(
                    onClick = {
                        isOverflowExpanded = true
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(
                            id = R.string.more_options,
                        ),
                    )
                }

                DropdownMenu(
                    expanded = isOverflowExpanded,
                    onDismissRequest = {
                        isOverflowExpanded = false
                    },
                ) {
                    overflowActions.forEach { action ->
                        DropdownMenuItem(
                            text = {
                                Text(text = selectionActionLabel(action = action))
                            },
                            onClick = {
                                isOverflowExpanded = false
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
        },
    )
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
        ConversationMessageSelectionAction.Share -> {
            stringResource(R.string.action_share)
        }
    }
}
