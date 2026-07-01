package com.android.messaging.ui.conversationlist.ui.topbar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.conversationlist.model.ConversationListAction as Action
import com.android.messaging.ui.core.MessagingPreviewTheme

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
    OverflowMenu { dismiss ->
        OverflowMenuItem(
            labelResId = R.string.action_menu_show_archived,
            onClick = {
                onAction(Action.ArchivedConversationsClicked)
                dismiss()
            },
        )

        if (hasBlockedParticipants) {
            OverflowMenuItem(
                labelResId = R.string.blocked_contacts_title,
                onClick = {
                    onAction(Action.BlockedParticipantsClicked)
                    dismiss()
                },
            )
        }

        OverflowMenuItem(
            labelResId = R.string.action_settings,
            onClick = {
                onAction(Action.SettingsClicked)
                dismiss()
            },
        )

        if (isDebugEnabled) {
            OverflowMenuItem(
                labelResId = R.string.action_debug_options,
                onClick = {
                    onAction(Action.DebugOptionsClicked)
                    dismiss()
                },
            )
        }
    }
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
