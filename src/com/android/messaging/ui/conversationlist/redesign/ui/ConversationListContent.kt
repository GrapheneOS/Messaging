package com.android.messaging.ui.conversationlist.redesign.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListItemUiModel
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList

private const val CONVERSATION_ROW_CONTENT_TYPE = "conversation_row"

private val ListVerticalSpacing = 2.dp

private val ListContentPadding = 8.dp

private val EmptyTextHorizontalPadding = 32.dp

@Composable
internal fun ConversationListContent(
    content: ConversationListContentUiState,
    listState: LazyListState,
    onAction: (Action) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    bottomReserve: Dp = 0.dp,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (content) {
            ConversationListContentUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            ConversationListContentUiState.WaitingForSync -> {
                ConversationListMessage(
                    text = stringResource(R.string.conversation_list_first_sync_text),
                )
            }

            ConversationListContentUiState.Empty -> {
                ConversationListMessage(
                    text = stringResource(R.string.conversation_list_empty_text),
                )
            }

            is ConversationListContentUiState.Items -> {
                ConversationListItems(
                    items = content.items,
                    listState = listState,
                    onAction = onAction,
                    contentPadding = contentPadding,
                    isSelectionMode = isSelectionMode,
                    bottomReserve = bottomReserve,
                )
            }
        }
    }
}

@Composable
private fun ConversationListItems(
    items: ImmutableList<ConversationListItemUiModel>,
    listState: LazyListState,
    onAction: (Action) -> Unit,
    contentPadding: PaddingValues,
    isSelectionMode: Boolean,
    bottomReserve: Dp,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(CONVERSATION_LIST_TEST_TAG),
        state = listState,
        contentPadding = PaddingValues(
            start = ListContentPadding,
            end = ListContentPadding,
            top = ListContentPadding,
            bottom = contentPadding.calculateBottomPadding() + ListContentPadding + bottomReserve,
        ),
        verticalArrangement = Arrangement.spacedBy(ListVerticalSpacing),
    ) {
        items(
            items = items,
            key = { item -> item.conversationId },
            contentType = { CONVERSATION_ROW_CONTENT_TYPE },
        ) { item ->
            val destination = item.avatar.normalizedDestination

            SwipeableConversationListItem(
                item = item,
                isSelectionMode = isSelectionMode,
                onArchive = {
                    onAction(Action.ConversationSwipedToArchive(item.conversationId))
                },
                onToggleRead = {
                    onAction(Action.ConversationSwipedToToggleRead(item.conversationId))
                },
                modifier = Modifier.animateItem(),
            ) {
                ConversationListItemRow(
                    item = item,
                    onClick = {
                        onAction(Action.ConversationClicked(item.conversationId))
                    },
                    onLongClick = {
                        onAction(Action.ConversationLongClicked(item.conversationId))
                    },
                    isSelectionMode = isSelectionMode,
                    onAvatarMessageClick = {
                        onAction(Action.AvatarMessageClicked(item.conversationId))
                    },
                    onAvatarCallClick = {
                        if (destination != null) {
                            onAction(Action.AvatarCallClicked(destination))
                        }
                    }.takeIf { item.avatar.canCall },
                    onAvatarContactClick = {
                        onAction(Action.AvatarContactClicked(item.avatar))
                    }.takeIf { item.avatar.canShowContact },
                )
            }
        }
    }
}

@Composable
private fun ConversationListMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = EmptyTextHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationListContentEmptyPreview() {
    MessagingPreviewTheme {
        ConversationListContent(
            content = ConversationListContentUiState.Empty,
            listState = rememberLazyListState(),
            onAction = {},
            contentPadding = PaddingValues(),
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationListContentWaitingForSyncPreview() {
    MessagingPreviewTheme {
        ConversationListContent(
            content = ConversationListContentUiState.WaitingForSync,
            listState = rememberLazyListState(),
            onAction = {},
            contentPadding = PaddingValues(),
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationListContentItemsPreview() {
    MessagingPreviewTheme {
        ConversationListContent(
            content = ConversationListContentUiState.Items(
                items = previewConversationListItems(),
            ),
            listState = rememberLazyListState(),
            onAction = {},
            contentPadding = PaddingValues(),
        )
    }
}
