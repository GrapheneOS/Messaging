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
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.common.components.PrimaryActionButton
import com.android.messaging.ui.common.components.reorder.OverlayReorderAnimationController
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction.AvatarCallClicked
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction.AvatarContactClicked
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction.AvatarInfoClicked
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction.AvatarMessageClicked
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction.ConversationClicked
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction.ConversationLongClicked
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction.ConversationSwipedToArchive
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction.ConversationSwipedToDelete
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction.ConversationSwipedToToggleRead
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction.StartChatClicked
import com.android.messaging.ui.conversationlist.chats.model.ConversationListUiState
import com.android.messaging.ui.conversationlist.common.item.ConversationSwipeKind
import com.android.messaging.ui.conversationlist.common.list.ConversationListItemEvent
import com.android.messaging.ui.conversationlist.common.list.ConversationListItems
import com.android.messaging.ui.conversationlist.common.list.ConversationListSwipeSpec
import com.android.messaging.ui.conversationlist.common.list.unsupportedSwipeKind
import com.android.messaging.ui.conversationlist.common.status.ConversationListLoadingIndicator
import com.android.messaging.ui.conversationlist.common.status.ConversationListStatusMessage
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItems
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel as Model
import com.android.messaging.ui.core.MessagingPreviewTheme

@Composable
internal fun ConversationListContent(
    content: ConversationListContentUiState,
    listState: LazyListState,
    onAction: (Action) -> Unit,
    scaffoldContentPadding: PaddingValues,
    isSelectionMode: Boolean,
    fabBottomReserve: Dp,
    pinAnimationController: OverlayReorderAnimationController<Model, ConversationId>?,
    swipeSpec: ConversationListSwipeSpec,
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
                            onClick = { onAction(StartChatClicked) },
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
                    swipeSpec = swipeSpec,
                    onItemEvent = { onAction(it.toChatAction()) },
                )
            }
        }
    }
}

private fun ConversationListItemEvent.toChatAction(): Action {
    return when (this) {
        is ConversationListItemEvent.Clicked -> {
            ConversationClicked(conversationId)
        }

        is ConversationListItemEvent.LongClicked -> {
            ConversationLongClicked(conversationId)
        }

        is ConversationListItemEvent.AvatarMessageClicked -> {
            AvatarMessageClicked(conversationId)
        }

        is ConversationListItemEvent.AvatarCallClicked -> {
            AvatarCallClicked(destination)
        }

        is ConversationListItemEvent.AvatarContactClicked -> {
            AvatarContactClicked(item.avatar)
        }

        is ConversationListItemEvent.AvatarInfoClicked -> {
            AvatarInfoClicked(conversationId)
        }

        is ConversationListItemEvent.Swiped -> when (kind) {
            ConversationSwipeKind.ToggleRead -> {
                ConversationSwipedToToggleRead(conversationId)
            }

            ConversationSwipeKind.Archive -> {
                ConversationSwipedToArchive(conversationId)
            }

            ConversationSwipeKind.Delete -> {
                ConversationSwipedToDelete(conversationId)
            }

            ConversationSwipeKind.Unarchive -> unsupportedSwipeKind(kind)
        }
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
            scaffoldContentPadding = PaddingValues(),
            isSelectionMode = false,
            fabBottomReserve = 0.dp,
            pinAnimationController = null,
            swipeSpec = ConversationListUiState().swipeSpec,
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
            swipeSpec = ConversationListUiState().swipeSpec,
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
            swipeSpec = ConversationListUiState().swipeSpec,
        )
    }
}
