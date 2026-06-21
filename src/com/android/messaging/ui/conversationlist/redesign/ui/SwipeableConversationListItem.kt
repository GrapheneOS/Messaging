package com.android.messaging.ui.conversationlist.redesign.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListItemUiModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val SwipeBackgroundShape = RoundedCornerShape(percent = 50)

private val SwipeBackgroundHorizontalPadding = 24.dp

private val SwipeActionThreshold = 88.dp

private enum class ConversationSwipeAction {
    Archive,
    ToggleRead,
    None,
}

@Composable
internal fun SwipeableConversationListItem(
    item: ConversationListItemUiModel,
    isSelectionMode: Boolean,
    onArchive: () -> Unit,
    onToggleRead: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val currentOnArchive by rememberUpdatedState(onArchive)
    val currentOnToggleRead by rememberUpdatedState(onToggleRead)
    val coroutineScope = rememberCoroutineScope()

    val offsetX = remember { Animatable(0f) }
    val draggedOffsetX = remember { mutableFloatStateOf(0f) }
    val backgroundAction by remember { derivedStateOf { swipeAction(offsetX.value) } }

    val thresholdPx = with(density) { SwipeActionThreshold.toPx() }

    val gestureModifier = when {
        isSelectionMode -> Modifier

        else -> Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    draggedOffsetX.floatValue += dragAmount
                    coroutineScope.launch { offsetX.snapTo(draggedOffsetX.floatValue) }
                },
                onDragEnd = {
                    when {
                        draggedOffsetX.floatValue <= -thresholdPx -> currentOnArchive()
                        draggedOffsetX.floatValue >= thresholdPx -> currentOnToggleRead()
                    }

                    draggedOffsetX.floatValue = 0f
                    coroutineScope.launch { offsetX.animateTo(0f) }
                },
                onDragCancel = {
                    draggedOffsetX.floatValue = 0f
                    coroutineScope.launch { offsetX.animateTo(0f) }
                },
            )
        }
    }

    Box(modifier = modifier.then(gestureModifier)) {
        ConversationListSwipeBackground(
            action = backgroundAction,
            isUnread = item.isUnread,
            modifier = Modifier.matchParentSize(),
        )

        Box(
            modifier = Modifier.offset {
                IntOffset(
                    x = offsetX.value.roundToInt(),
                    y = 0,
                )
            },
        ) {
            content()
        }
    }
}

private fun swipeAction(offset: Float): ConversationSwipeAction {
    return when {
        offset > 0f -> ConversationSwipeAction.ToggleRead
        offset < 0f -> ConversationSwipeAction.Archive
        else -> ConversationSwipeAction.None
    }
}

@Composable
private fun ConversationListSwipeBackground(
    action: ConversationSwipeAction,
    isUnread: Boolean,
    modifier: Modifier = Modifier,
) {
    if (action == ConversationSwipeAction.None) {
        Box(modifier = modifier)
        return
    }

    val isArchive = action == ConversationSwipeAction.Archive

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
        modifier = modifier
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
