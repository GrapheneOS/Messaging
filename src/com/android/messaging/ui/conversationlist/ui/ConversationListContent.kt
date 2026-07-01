package com.android.messaging.ui.conversationlist.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
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
import com.android.messaging.ui.common.components.PrimaryActionButton
import com.android.messaging.ui.common.components.horizontalSafeDrawingInsets
import com.android.messaging.ui.common.components.reorder.OverlayReorderAnimationController
import com.android.messaging.ui.conversationlist.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.ui.item.ConversationListItemRow
import com.android.messaging.ui.conversationlist.ui.item.SwipeableConversationListItem
import com.android.messaging.ui.conversationlist.ui.support.AppearanceAnimationToken
import com.android.messaging.ui.conversationlist.ui.support.CONVERSATION_LIST_TEST_TAG
import com.android.messaging.ui.conversationlist.ui.support.previewConversationListItems
import com.android.messaging.ui.conversationlist.ui.support.rememberAppearanceAnimationTokens
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

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
    scaffoldContentPadding: PaddingValues,
    isSelectionMode: Boolean,
    fabBottomReserve: Dp,
    pinAnimationController: OverlayReorderAnimationController<ConversationListItemUiModel, String>?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (content) {
            ConversationListContentUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                    onAction = onAction,
                    scaffoldContentPadding = scaffoldContentPadding,
                    isSelectionMode = isSelectionMode,
                    fabBottomReserve = fabBottomReserve,
                    pinAnimationController = pinAnimationController,
                )
            }
        }
    }
}

@Composable
private fun ConversationListItems(
    items: ImmutableList<ConversationListItemUiModel>,
    restoredConversationIds: ImmutableSet<String>,
    listState: LazyListState,
    onAction: (Action) -> Unit,
    scaffoldContentPadding: PaddingValues,
    isSelectionMode: Boolean,
    fabBottomReserve: Dp,
    pinAnimationController: OverlayReorderAnimationController<ConversationListItemUiModel, String>?,
) {
    val rowHorizontalInsets = horizontalSafeDrawingInsets()

    val appearanceTokens = rememberAppearanceAnimationTokens(
        items = items,
        listState = listState,
        excludedConversationIds = restoredConversationIds,
    )

    SideEffect {
        pinAnimationController?.updateItems(items)
    }

    KeepViewportStationaryOnPinChange(
        listState = listState,
        items = items,
        restoredConversationIds = restoredConversationIds,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(CONVERSATION_LIST_TEST_TAG),
        state = listState,
        contentPadding = PaddingValues(
            top = ListContentPadding,
            bottom = scaffoldContentPadding.calculateBottomPadding() +
                ListContentPadding +
                fabBottomReserve,
        ),
        verticalArrangement = Arrangement.spacedBy(ListVerticalSpacing),
    ) {
        items(
            items = items,
            key = { item -> item.conversationId },
            contentType = { CONVERSATION_ROW_CONTENT_TYPE },
        ) { item ->
            val appearanceAnimationToken = appearanceTokens.tokenFor(item.conversationId)

            ConversationListRow(
                item = item,
                listState = listState,
                isSelectionMode = isSelectionMode,
                horizontalInsets = rowHorizontalInsets,
                appearanceAnimationToken = appearanceAnimationToken,
                pinAnimationController = pinAnimationController,
                onAppearanceAnimationFinished = {
                    if (appearanceAnimationToken != null) {
                        appearanceTokens.onAnimationFinished(
                            conversationId = item.conversationId,
                            token = appearanceAnimationToken,
                        )
                    }
                },
                onAction = onAction,
            )
        }
    }
}

internal fun Modifier.conversationRowHorizontalPadding(horizontalInsets: PaddingValues): Modifier {
    return padding(horizontalInsets)
        .padding(horizontal = ListContentPadding)
}

@Composable
private fun LazyItemScope.ConversationListRow(
    item: ConversationListItemUiModel,
    listState: LazyListState,
    isSelectionMode: Boolean,
    horizontalInsets: PaddingValues,
    appearanceAnimationToken: AppearanceAnimationToken?,
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
        isInteractionEnabled = !isHiddenByPinAnimation,
        appearanceAnimationToken = appearanceAnimationToken,
        onAppearanceAnimationFinished = onAppearanceAnimationFinished,
        onArchive = {
            onAction(Action.ConversationSwipedToArchive(item.conversationId))
        },
        onToggleRead = {
            onAction(Action.ConversationSwipedToToggleRead(item.conversationId))
        },
        backgroundHorizontalInsets = horizontalInsets,
        modifier = Modifier.conversationRowSwipeModifier(
            lazyItemScope = this,
            item = item,
            listState = listState,
            pinAnimationController = pinAnimationController,
            isHidden = isHiddenByPinAnimation,
        ),
    ) {
        ConversationListItemRow(
            item = item,
            modifier = Modifier.conversationRowHorizontalPadding(horizontalInsets),
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

private fun Modifier.conversationRowSwipeModifier(
    lazyItemScope: LazyItemScope,
    item: ConversationListItemUiModel,
    listState: LazyListState,
    pinAnimationController: OverlayReorderAnimationController<ConversationListItemUiModel, String>?,
    isHidden: Boolean,
): Modifier {
    return conversationItemAnimation(
        lazyItemScope = lazyItemScope,
        isPinned = item.isPinned,
        animatePlacement = !isHidden,
    )
        .trackPinAnimationBounds(
            listState = listState,
            conversationId = item.conversationId,
            pinAnimationController = pinAnimationController,
        )
        .graphicsLayer {
            alpha = when {
                isHidden -> 0f
                else -> 1f
            }
        }
}

private fun Modifier.trackPinAnimationBounds(
    listState: LazyListState,
    conversationId: String,
    pinAnimationController: OverlayReorderAnimationController<ConversationListItemUiModel, String>?,
): Modifier {
    if (pinAnimationController == null) {
        return this
    }

    return onGloballyPositioned { coordinates ->
        val layoutInfo = listState.layoutInfo
        val physicallyVisibleItems = layoutInfo.visibleItemsInfo.filter { visibleItem ->
            visibleItem.offset < layoutInfo.viewportEndOffset &&
                visibleItem.offset + visibleItem.size > layoutInfo.viewportStartOffset
        }

        val firstVisibleItemIndex = physicallyVisibleItems
            .minOfOrNull { visibleItem -> visibleItem.index }
            ?: listState.firstVisibleItemIndex

        val lastVisibleItemIndex = physicallyVisibleItems
            .maxOfOrNull { visibleItem -> visibleItem.index }
            ?: firstVisibleItemIndex

        pinAnimationController.updateItemBounds(
            itemKey = conversationId,
            boundsInRoot = coordinates.boundsInRoot(),
            isPhysicallyVisible = physicallyVisibleItems.any { visibleItem ->
                visibleItem.key == conversationId
            },
            firstVisibleItemIndex = firstVisibleItemIndex,
            lastVisibleItemIndex = lastVisibleItemIndex,
        )
    }
}

@Composable
private fun KeepViewportStationaryOnPinChange(
    listState: LazyListState,
    items: ImmutableList<ConversationListItemUiModel>,
    restoredConversationIds: ImmutableSet<String>,
) {
    val previousItemsState = remember { mutableStateOf(items) }

    SideEffect {
        val previousItems = previousItemsState.value
        val firstVisibleConversationId = listState.layoutInfo
            .visibleItemsInfo
            .firstOrNull { visibleItem ->
                visibleItem.index == listState.firstVisibleItemIndex
            }
            ?.key as? String

        val scrollRequest = resolvePinChangeScrollRequest(
            previousItems = previousItems,
            currentItems = items,
            restoredConversationIds = restoredConversationIds,
            firstVisibleConversationId = firstVisibleConversationId,
            firstVisibleItemIndex = listState.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
        )

        if (scrollRequest != null) {
            listState.requestScrollToItem(
                index = scrollRequest.index,
                scrollOffset = scrollRequest.scrollOffset,
            )
        }

        previousItemsState.value = items
    }
}

internal data class ConversationListScrollRequest(
    val index: Int,
    val scrollOffset: Int,
)

internal fun resolvePinChangeScrollRequest(
    previousItems: List<ConversationListItemUiModel>,
    currentItems: List<ConversationListItemUiModel>,
    restoredConversationIds: Set<String>,
    firstVisibleConversationId: String?,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
): ConversationListScrollRequest? {
    val currentItemsById = currentItems.associateBy(ConversationListItemUiModel::conversationId)
    val previousConversationIds = previousItems.mapTo(HashSet()) { it.conversationId }
    val isAtTop = firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
    val currentTopConversationId = currentItems.firstOrNull()?.conversationId
    val isNewTopConversation = currentTopConversationId != null &&
        currentTopConversationId !in previousConversationIds &&
        currentTopConversationId !in restoredConversationIds

    return when {
        isAtTop && isNewTopConversation -> {
            ConversationListScrollRequest(
                index = 0,
                scrollOffset = 0,
            )
        }

        !hasPinReorder(previousItems, currentItemsById) -> null

        isAtTop -> {
            ConversationListScrollRequest(
                index = 0,
                scrollOffset = 0,
            )
        }

        else -> resolveAnchorScrollRequest(
            previousItems = previousItems,
            currentItemsById = currentItemsById,
            firstVisibleConversationId = firstVisibleConversationId,
            firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
        )
    }
}

private fun hasPinReorder(
    previousItems: List<ConversationListItemUiModel>,
    currentItemsById: Map<String, ConversationListItemUiModel>,
): Boolean {
    val hasSameConversationIds = previousItems.size == currentItemsById.size &&
        previousItems.all { item -> item.conversationId in currentItemsById }

    return hasSameConversationIds && previousItems.any { previousItem ->
        currentItemsById.getValue(previousItem.conversationId).isPinned != previousItem.isPinned
    }
}

private fun resolveAnchorScrollRequest(
    previousItems: List<ConversationListItemUiModel>,
    currentItemsById: Map<String, ConversationListItemUiModel>,
    firstVisibleConversationId: String?,
    firstVisibleItemScrollOffset: Int,
): ConversationListScrollRequest? {
    val previousFirstVisibleIndex = previousItems.indexOfFirst { item ->
        item.conversationId == firstVisibleConversationId
    }
    val previousFirstVisibleItem = previousItems.getOrNull(previousFirstVisibleIndex)
    val currentFirstVisibleItem = previousFirstVisibleItem
        ?.let { currentItemsById[it.conversationId] }

    val hasAnchorPinChange = previousFirstVisibleItem != null &&
        currentFirstVisibleItem != null &&
        previousFirstVisibleItem.isPinned != currentFirstVisibleItem.isPinned

    return ConversationListScrollRequest(
        index = previousFirstVisibleIndex,
        scrollOffset = firstVisibleItemScrollOffset,
    ).takeIf { hasAnchorPinChange }
}

private fun Modifier.conversationItemAnimation(
    lazyItemScope: LazyItemScope,
    isPinned: Boolean,
    animatePlacement: Boolean,
): Modifier = with(lazyItemScope) {
    this@conversationItemAnimation
        .zIndex(
            when {
                isPinned -> PINNED_ITEM_Z_INDEX
                else -> 0f
            },
        )
        .animateItem(
            fadeInSpec = null,
            fadeOutSpec = null,
            placementSpec = when {
                animatePlacement -> ItemPlacementSpec
                else -> null
            },
        )
}

@Composable
private fun ConversationListStatusMessage(
    text: String,
    actionButton: @Composable () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = EmptyTextHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.large,
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            actionButton()
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
