package com.android.messaging.ui.conversationlist.redesign.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListItemUiModel
import kotlinx.coroutines.launch

private val SwipeBackgroundShape = RoundedCornerShape(percent = 50)

private val SwipeBackgroundHorizontalPadding = 24.dp

@Composable
internal fun SwipeableConversationListItem(
    item: ConversationListItemUiModel,
    isSelectionMode: Boolean,
    onArchive: () -> Unit,
    onToggleRead: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentOnArchive by rememberUpdatedState(onArchive)
    val currentOnToggleRead by rememberUpdatedState(onToggleRead)
    val coroutineScope = rememberCoroutineScope()

    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    val dismissState = remember {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            positionalThreshold = positionalThreshold,
        )
    }

    val onDismiss = remember(dismissState, coroutineScope) {
        { direction: SwipeToDismissBoxValue ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    currentOnToggleRead()
                    coroutineScope.launch { dismissState.reset() }
                    Unit
                }

                SwipeToDismissBoxValue.EndToStart -> currentOnArchive()

                SwipeToDismissBoxValue.Settled -> Unit
            }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = !isSelectionMode,
        enableDismissFromEndToStart = !isSelectionMode,
        onDismiss = onDismiss,
        backgroundContent = {
            ConversationListSwipeBackground(
                direction = dismissState.dismissDirection,
                isUnread = item.isUnread,
            )
        },
        content = { content() },
    )
}

@Composable
private fun ConversationListSwipeBackground(
    direction: SwipeToDismissBoxValue,
    isUnread: Boolean,
) {
    if (direction == SwipeToDismissBoxValue.Settled) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val isArchive = direction == SwipeToDismissBoxValue.EndToStart

    val containerColor = when {
        isArchive -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val contentColor = when {
        isArchive -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    val alignment = when {
        isArchive -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    val icon = when {
        isArchive -> Icons.Filled.Archive
        isUnread -> Icons.Filled.MarkChatRead
        else -> Icons.Filled.MarkChatUnread
    }

    val description = when {
        isArchive -> stringResource(R.string.action_archive)
        isUnread -> stringResource(R.string.mark_as_read)
        else -> stringResource(R.string.mark_as_unread)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(SwipeBackgroundShape)
            .background(containerColor)
            .padding(horizontal = SwipeBackgroundHorizontalPadding),
        contentAlignment = alignment,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = contentColor,
        )
    }
}
