package com.android.messaging.ui.conversationlist.chats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.PrimaryActionButton
import com.android.messaging.ui.common.components.reorder.OverlayReorderAnimationController
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.common.item.ConversationSwipeAction
import com.android.messaging.ui.conversationlist.common.item.ConversationSwipeBackground
import com.android.messaging.ui.conversationlist.common.list.ConversationListItemCallbacks
import com.android.messaging.ui.conversationlist.common.list.ConversationListItems
import com.android.messaging.ui.conversationlist.common.list.ConversationListSwipeActions
import com.android.messaging.ui.conversationlist.common.status.ConversationListLoadingIndicator
import com.android.messaging.ui.conversationlist.common.status.ConversationListStatusMessage
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItems
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.core.MessagingPreviewTheme

@Composable
internal fun ConversationListContent(
    content: ConversationListContentUiState,
    listState: LazyListState,
    onAction: (Action) -> Unit,
    scaffoldContentPadding: PaddingValues,
    isSelectionMode: Boolean,
    fabBottomReserve: Dp,
    pinAnimationController: OverlayReorderAnimationController<ConversationListItemUiModel, String>?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (content) {
            ConversationListContentUiState.Loading -> {
                ConversationListLoadingIndicator()
            }

            ConversationListContentUiState.WaitingForSync -> {
                ConversationListStatusMessage(
                    text = stringResource(R.string.conversation_list_first_sync_text),
                )
            }

            ConversationListContentUiState.Empty -> {
                ConversationListStatusMessage(
                    text = stringResource(R.string.conversation_list_empty_text),
                    actionButton = {
                        PrimaryActionButton(
                            text = stringResource(R.string.conversation_list_start_chat),
                            onClick = { onAction(Action.StartChatClicked) },
                            leadingIcon = Icons.AutoMirrored.Rounded.Chat,
                        )
                    },
                )
            }

            is ConversationListContentUiState.Items -> {
                ConversationListItems(
                    items = content.items,
                    restoredConversationIds = content.restoredConversationIds,
                    listState = listState,
                    isSelectionMode = isSelectionMode,
                    scaffoldContentPadding = scaffoldContentPadding,
                    fabBottomReserve = fabBottomReserve,
                    pinAnimationController = pinAnimationController,
                    callbacks = inboxCallbacks(onAction),
                    swipeActions = { item -> inboxSwipeActions(item.conversationId, onAction) },
                )
            }
        }
    }
}

private fun inboxCallbacks(onAction: (Action) -> Unit): ConversationListItemCallbacks {
    return ConversationListItemCallbacks(
        onClick = { onAction(Action.ConversationClicked(it)) },
        onLongClick = { onAction(Action.ConversationLongClicked(it)) },
        onAvatarMessageClick = { onAction(Action.AvatarMessageClicked(it)) },
        onAvatarCallClick = { onAction(Action.AvatarCallClicked(it)) },
        onAvatarContactClick = { onAction(Action.AvatarContactClicked(it.avatar)) },
        onAvatarInfoClick = { onAction(Action.AvatarInfoClicked(it)) },
    )
}

private fun inboxSwipeActions(
    conversationId: String,
    onAction: (Action) -> Unit,
): ConversationListSwipeActions {
    return ConversationListSwipeActions(
        startToEnd = ConversationSwipeAction(
            background = ConversationSwipeBackground.ToggleRead,
            onTrigger = { onAction(Action.ConversationSwipedToToggleRead(conversationId)) },
        ),
        endToStart = ConversationSwipeAction(
            background = ConversationSwipeBackground.Archive,
            onTrigger = { onAction(Action.ConversationSwipedToArchive(conversationId)) },
        ),
    )
}

@PreviewLightDark
@Composable
private fun ConversationListContentEmptyPreview() {
    MessagingPreviewTheme {
        ConversationListContent(
            content = ConversationListContentUiState.Empty,
            listState = rememberLazyListState(),
            onAction = {},
            scaffoldContentPadding = PaddingValues(),
            isSelectionMode = false,
            fabBottomReserve = 0.dp,
            pinAnimationController = null,
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
            scaffoldContentPadding = PaddingValues(),
            isSelectionMode = false,
            fabBottomReserve = 0.dp,
            pinAnimationController = null,
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
            scaffoldContentPadding = PaddingValues(),
            isSelectionMode = false,
            fabBottomReserve = 0.dp,
            pinAnimationController = null,
        )
    }
}
