package com.android.messaging.ui.conversationlist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversationlist.model.ConversationListAction as Action
import com.android.messaging.ui.core.MessagingPreviewTheme

private val OverflowMenuWidth = 220.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationListTopAppBar(
    hasBlockedParticipants: Boolean,
    isDebugEnabled: Boolean,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        title = {
            Text(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    onAction(Action.ScrollToTopClicked)
                },
                text = stringResource(R.string.app_name),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            ConversationListOverflowMenu(
                hasBlockedParticipants = hasBlockedParticipants,
                isDebugEnabled = isDebugEnabled,
                onAction = onAction,
            )
        },
    )
}

@Composable
private fun ConversationListOverflowMenu(
    hasBlockedParticipants: Boolean,
    isDebugEnabled: Boolean,
    onAction: (Action) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.more_options),
            )
        }

        DropdownMenu(
            modifier = Modifier.width(OverflowMenuWidth),
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            ConversationListMenuItem(
                labelResId = R.string.action_menu_show_archived,
                onClick = {
                    onAction(Action.ArchivedConversationsClicked)
                    isExpanded = false
                },
            )

            if (hasBlockedParticipants) {
                ConversationListMenuItem(
                    labelResId = R.string.blocked_contacts_title,
                    onClick = {
                        onAction(Action.BlockedParticipantsClicked)
                        isExpanded = false
                    },
                )
            }

            ConversationListMenuItem(
                labelResId = R.string.action_settings,
                onClick = {
                    onAction(Action.SettingsClicked)
                    isExpanded = false
                },
            )

            if (isDebugEnabled) {
                ConversationListMenuItem(
                    labelResId = R.string.action_debug_options,
                    onClick = {
                        onAction(Action.DebugOptionsClicked)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ConversationListMenuItem(
    labelResId: Int,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(text = stringResource(labelResId))
        },
        onClick = onClick,
    )
}

@PreviewLightDark
@Composable
private fun ConversationListTopAppBarPreview() {
    MessagingPreviewTheme {
        ConversationListTopAppBar(
            hasBlockedParticipants = true,
            isDebugEnabled = true,
            onAction = {},
        )
    }
}
