package com.android.messaging.ui.conversation.screen

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.messages.ui.message.MessageHighlightDuration
import kotlinx.coroutines.delay

private const val SMOOTH_SCROLL_JUMP_THRESHOLD = 15

/**
 * A list that opens at the target has not been seen yet, so it goes straight to the target, while
 * one already on screen scrolls there.
 *
 * The target stays pending while the window grows to bring it in, so the effect runs again on the
 * larger list. It is consumed only once it is on screen, or once the delegate reports it missing:
 * a deleted id must not keep the screen waiting.
 */
@Composable
internal fun ScrollToTargetMessage(
    conversationId: ConversationId?,
    pendingScrollMessageId: MessageId?,
    messagesState: ConversationMessagesUiState.Present,
    listState: LazyListState,
    leadingItemCount: Int,
    isListUnseen: Boolean,
    onLoadMessage: (MessageId) -> Unit,
    onTargetPlaced: () -> Unit,
    onTargetMessageShown: (MessageId) -> Unit,
    onConsumed: () -> Unit,
) {
    val messages = messagesState.messages
    val missingMessageId = messagesState.missingMessageId

    // A moved window can hold as many messages as the one before, and comes with a list of its own.
    LaunchedEffect(
        conversationId,
        pendingScrollMessageId,
        listState,
        messages.size,
        missingMessageId,
        leadingItemCount,
    ) {
        if (pendingScrollMessageId == null) {
            return@LaunchedEffect
        }

        val listIndex = targetMessageListIndex(
            messages = messages,
            messageId = pendingScrollMessageId,
            leadingItemCount = leadingItemCount,
        )

        if (listIndex == null) {
            when (missingMessageId) {
                pendingScrollMessageId -> onConsumed()
                else -> onLoadMessage(pendingScrollMessageId)
            }
            return@LaunchedEffect
        }

        when {
            isListUnseen -> listState.scrollToCenteredItem(index = listIndex, isAnimated = false)

            else -> {
                listState.scrollCloseTo(
                    index = listIndex,
                    lastIndex = leadingItemCount + messages.lastIndex,
                )
                listState.scrollToCenteredItem(index = listIndex, isAnimated = true)
            }
        }

        onTargetPlaced()
        onTargetMessageShown(pendingScrollMessageId)
        onConsumed()
    }
}

/** Jumps to within a short animated scroll of [index]. */
private suspend fun LazyListState.scrollCloseTo(index: Int, lastIndex: Int) {
    val delta = index - firstVisibleItemIndex

    val intermediateIndex = when {
        delta > SMOOTH_SCROLL_JUMP_THRESHOLD -> index - SMOOTH_SCROLL_JUMP_THRESHOLD
        delta < -SMOOTH_SCROLL_JUMP_THRESHOLD -> index + SMOOTH_SCROLL_JUMP_THRESHOLD
        else -> return
    }

    scrollToItem(index = intermediateIndex.coerceIn(0, lastIndex))
}

/**
 * Where [messageId] is in the list, or null when it is not one of [messages]. The list renders the
 * messages reversed, so the newest one comes right after the leading items.
 */
internal fun targetMessageListIndex(
    messages: List<ConversationMessageUiModel>,
    messageId: MessageId?,
    leadingItemCount: Int,
): Int? {
    val messageIndex = messages.indexOfFirst { message -> message.messageId == messageId }

    return when {
        messageId == null || messageIndex < 0 -> null
        else -> leadingItemCount + messages.lastIndex - messageIndex
    }
}

/**
 * The item's height is only known once it is laid out, so the first pass may centre its edge and
 * the second corrects by half its height. At either end of the list the scroll stops short.
 */
private suspend fun LazyListState.scrollToCenteredItem(index: Int, isAnimated: Boolean) {
    repeat(times = 2) {
        val layoutInfo = layoutInfo
        val itemSize = layoutInfo
            .visibleItemsInfo
            .firstOrNull { item -> item.index == index }
            ?.size
            ?: 0
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2

        val scrollOffset = itemSize / 2 - viewportCenter

        when {
            isAnimated -> animateScrollToItem(index = index, scrollOffset = scrollOffset)
            else -> scrollToItem(index = index, scrollOffset = scrollOffset)
        }
    }
}

@Composable
internal fun EndMessageHighlight(
    highlightedMessageId: MessageId?,
    onMessageHighlightFinished: (MessageId) -> Unit,
) {
    LaunchedEffect(highlightedMessageId) {
        val messageId = highlightedMessageId ?: return@LaunchedEffect

        delay(duration = MessageHighlightDuration)
        onMessageHighlightFinished(messageId)
    }
}
