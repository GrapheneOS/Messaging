package com.android.messaging.ui.conversationlist.archived

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListAction as Action
import com.android.messaging.ui.core.MessagingPreviewTheme

private val OverflowMenuWidth = 220.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArchivedConversationListTopAppBar(
    isDebugEnabled: Boolean,
    onNavigateBack: () -> Unit,
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
                text = stringResource(R.string.archived_activity_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        actions = {
            if (isDebugEnabled) {
                ArchivedConversationListOverflowMenu(onAction = onAction)
            }
        },
    )
}

@Composable
private fun ArchivedConversationListOverflowMenu(
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
            DropdownMenuItem(
                text = {
                    Text(text = stringResource(R.string.action_debug_options))
                },
                onClick = {
                    onAction(Action.DebugOptionsClicked)
                    isExpanded = false
                },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ArchivedConversationListTopAppBarPreview() {
    MessagingPreviewTheme {
        ArchivedConversationListTopAppBar(
            isDebugEnabled = true,
            onNavigateBack = {},
            onAction = {},
        )
    }
}
