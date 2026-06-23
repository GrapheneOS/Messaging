package com.android.messaging.ui.conversationlist.redesign.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.messaging.R
import com.android.messaging.ui.common.components.reorder.OverlayReorderAnimationController
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListItemUiModel
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList

private const val CONVERSATION_ROW_CONTENT_TYPE = "conversation_row"

private const val PINNED_ITEM_Z_INDEX = 1f

private val ItemPlacementSpec = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)

private val ListVerticalSpacing = 2.dp

private val ListContentPadding = 8.dp

private val EmptyTextHorizontalPadding = 32.dp

@Composable
internal fun ConversationListContent(
    content: ConversationListContentUiState,
    listState: LazyListState,
    onAction: (Action) -> Unit,
    contentPadding: PaddingValues,
    isSelectionMode: Boolean,
    bottomReserve: Dp,
    pinAnimationController: OverlayReorderAnimationController<ConversationListItemUiModel, String>?,
    modifier: Modifier = Modifier,
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
                    pinAnimationController = pinAnimationController,
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
    pinAnimationController: OverlayReorderAnimationController<ConversationListItemUiModel, String>?,
) {
    val currentConversationIds: Set<String> =
        items.mapTo(mutableSetOf(), ConversationListItemUiModel::conversationId)
    val previousConversationIdsState = remember { mutableStateOf(currentConversationIds) }
    val enteringConversationIds = currentConversationIds - previousConversationIdsState.value
    val appearanceGenerationById = remember { mutableMapOf<String, Long>() }
    val activeAppearanceTokens = remember { mutableStateMapOf<String, Long>() }
    val enteringAppearanceTokens = remember(items, enteringConversationIds) {
        enteringConversationIds.associateWith { conversationId ->
            appearanceGenerationById.getOrDefault(conversationId, 0L) + 1L
        }.also(appearanceGenerationById::putAll)
    }

    SideEffect {
        previousConversationIdsState.value = currentConversationIds
        activeAppearanceTokens.putAll(enteringAppearanceTokens)
        pinAnimationController?.updateItems(items)
    }

    KeepViewportStationaryOnPinChange(
        listState = listState,
        items = items,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(CONVERSATION_LIST_TEST_TAG),
        state = listState,
        contentPadding = PaddingValues(
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
            val appearanceAnimationToken = enteringAppearanceTokens[item.conversationId]
                ?: activeAppearanceTokens[item.conversationId]

            ConversationListRow(
                item = item,
                items = items,
                listState = listState,
                isSelectionMode = isSelectionMode,
                appearanceAnimationToken = appearanceAnimationToken,
                pinAnimationController = pinAnimationController,
                onAppearanceAnimationFinished = {
                    if (activeAppearanceTokens[item.conversationId] == appearanceAnimationToken) {
                        activeAppearanceTokens.remove(item.conversationId)
                    }
                },
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun LazyItemScope.ConversationListRow(
    item: ConversationListItemUiModel,
    items: ImmutableList<ConversationListItemUiModel>,
    listState: LazyListState,
    isSelectionMode: Boolean,
    appearanceAnimationToken: Long?,
    pinAnimationController: OverlayReorderAnimationController<ConversationListItemUiModel, String>?,
    onAppearanceAnimationFinished: () -> Unit,
    onAction: (Action) -> Unit,
) {
    val destination = item.avatar.normalizedDestination
    val isHiddenByPinAnimation = pinAnimationController?.isItemHidden(item.conversationId) == true

    DisposableEffect(item.conversationId, pinAnimationController) {
        onDispose {
            pinAnimationController?.removeItemBounds(item.conversationId)
        }
    }

    SwipeableConversationListItem(
        item = item,
        isSelectionMode = isSelectionMode,
        appearanceAnimationToken = appearanceAnimationToken,
        onAppearanceAnimationFinished = onAppearanceAnimationFinished,
        onArchive = {
            onAction(Action.ConversationSwipedToArchive(item.conversationId))
        },
        onToggleRead = {
            onAction(Action.ConversationSwipedToToggleRead(item.conversationId))
        },
        modifier = Modifier
            .conversationItemAnimation(
                lazyItemScope = this,
                isPinned = item.isPinned,
                animatePlacement = !isHiddenByPinAnimation,
            )
            .reportPinAnimationBounds(
                listState = listState,
                items = items,
                conversationId = item.conversationId,
                pinAnimationController = pinAnimationController,
            )
            .graphicsLayer { alpha = if (isHiddenByPinAnimation) 0f else 1f },
    ) {
        ConversationListItemRow(
            item = item,
            modifier = Modifier.padding(horizontal = ListContentPadding),
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
            onAvatarInfoClick = {
                onAction(Action.AvatarInfoClicked(item.conversationId))
            },
        )
    }
}

private fun Modifier.reportPinAnimationBounds(
    listState: LazyListState,
    items: ImmutableList<ConversationListItemUiModel>,
    conversationId: String,
    pinAnimationController: OverlayReorderAnimationController<ConversationListItemUiModel, String>?,
): Modifier {
    return onGloballyPositioned { coordinates ->
        val layoutInfo = listState.layoutInfo
        val physicallyVisibleItems = layoutInfo.visibleItemsInfo.filter { visibleItem ->
            visibleItem.offset < layoutInfo.viewportEndOffset &&
                visibleItem.offset + visibleItem.size > layoutInfo.viewportStartOffset
        }
        val lastVisibleItemIndex = physicallyVisibleItems
            .filter { visibleItem -> visibleItem.key != conversationId }
            .maxOfOrNull { visibleItem -> visibleItem.index }
            ?.let { lastIndex -> (lastIndex + 1).coerceAtMost(items.lastIndex) }
            ?: listState.firstVisibleItemIndex

        pinAnimationController?.updateItemBounds(
            itemKey = conversationId,
            boundsInRoot = coordinates.boundsInRoot(),
            isPhysicallyVisible = physicallyVisibleItems.any { visibleItem ->
                visibleItem.key == conversationId
            },
            firstVisibleItemIndex = listState.firstVisibleItemIndex,
            lastVisibleItemIndex = lastVisibleItemIndex,
        )
    }
}

@Composable
private fun KeepViewportStationaryOnPinChange(
    listState: LazyListState,
    items: ImmutableList<ConversationListItemUiModel>,
) {
    val previousItemsState = remember { mutableStateOf(items) }

    SideEffect {
        val previousItems = previousItemsState.value
        val currentItemsById = items.associateBy(ConversationListItemUiModel::conversationId)
        val hasSameConversationIds = previousItems.size == items.size && previousItems.all { item ->
            item.conversationId in currentItemsById
        }
        val firstVisibleConversationId = listState.layoutInfo
            .visibleItemsInfo
            .firstOrNull()
            ?.key as? String
        val previousFirstVisibleIndex = previousItems.indexOfFirst { item ->
            item.conversationId == firstVisibleConversationId
        }
        val previousFirstVisibleItem = previousItems.getOrNull(previousFirstVisibleIndex)
        val currentFirstVisibleItem = firstVisibleConversationId?.let(currentItemsById::get)
        val firstVisibleItemPinChanged = previousFirstVisibleItem?.isPinned !=
            currentFirstVisibleItem?.isPinned

        if (
            hasSameConversationIds &&
            previousFirstVisibleIndex >= 0 &&
            firstVisibleItemPinChanged
        ) {
            listState.requestScrollToItem(
                index = previousFirstVisibleIndex,
                scrollOffset = listState.firstVisibleItemScrollOffset,
            )
        }

        previousItemsState.value = items
    }
}

private fun Modifier.conversationItemAnimation(
    lazyItemScope: LazyItemScope,
    isPinned: Boolean,
    animatePlacement: Boolean,
): Modifier = with(lazyItemScope) {
    this@conversationItemAnimation
        .zIndex(if (isPinned) PINNED_ITEM_Z_INDEX else 0f)
        .animateItem(
            fadeInSpec = null,
            fadeOutSpec = null,
            placementSpec = if (animatePlacement) ItemPlacementSpec else null,
        )
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
            isSelectionMode = false,
            bottomReserve = 0.dp,
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
            contentPadding = PaddingValues(),
            isSelectionMode = false,
            bottomReserve = 0.dp,
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
            contentPadding = PaddingValues(),
            isSelectionMode = false,
            bottomReserve = 0.dp,
            pinAnimationController = null,
        )
    }
}
