package com.android.messaging.ui.conversationlist.common.list

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.messaging.ui.common.components.horizontalSafeDrawingInsets
import com.android.messaging.ui.common.components.reorder.OverlayReorderAnimationController
import com.android.messaging.ui.conversationlist.common.item.ConversationListItemRow
import com.android.messaging.ui.conversationlist.common.item.ConversationSwipeAction
import com.android.messaging.ui.conversationlist.common.item.SwipeableConversationListItem
import com.android.messaging.ui.conversationlist.common.support.AppearanceAnimationToken
import com.android.messaging.ui.conversationlist.common.support.CONVERSATION_LIST_TEST_TAG
import com.android.messaging.ui.conversationlist.common.support.rememberAppearanceAnimationTokens
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
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

internal data class ConversationListItemCallbacks(
    val onClick: (conversationId: String) -> Unit,
    val onLongClick: (conversationId: String) -> Unit,
    val onAvatarMessageClick: (conversationId: String) -> Unit,
    val onAvatarCallClick: (destination: String) -> Unit,
    val onAvatarContactClick: (item: ConversationListItemUiModel) -> Unit,
    val onAvatarInfoClick: (conversationId: String) -> Unit,
)

internal data class ConversationListSwipeActions(
    val startToEnd: ConversationSwipeAction?,
    val endToStart: ConversationSwipeAction?,
)

@Composable
internal fun ConversationListItems(
    items: ImmutableList<ConversationListItemUiModel>,
    restoredConversationIds: ImmutableSet<String>,
    listState: LazyListState,
    isSelectionMode: Boolean,
    scaffoldContentPadding: PaddingValues,
    fabBottomReserve: Dp,
    pinAnimationController: OverlayReorderAnimationController<ConversationListItemUiModel, String>?,
    callbacks: ConversationListItemCallbacks,
    swipeActions: (ConversationListItemUiModel) -> ConversationListSwipeActions,
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
                callbacks = callbacks,
                swipeActions = swipeActions(item),
            )
        }
    }
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
    callbacks: ConversationListItemCallbacks,
    swipeActions: ConversationListSwipeActions,
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
        startToEndAction = swipeActions.startToEnd,
        endToStartAction = swipeActions.endToStart,
        backgroundHorizontalInsets = horizontalInsets,
        modifier = Modifier
            .conversationItemAnimation(
                lazyItemScope = this,
                isPinned = item.isPinned,
                animatePlacement = !isHiddenByPinAnimation,
            )
            .trackPinAnimationBounds(
                listState = listState,
                conversationId = item.conversationId,
                pinAnimationController = pinAnimationController,
            )
            .graphicsLayer {
                alpha = when {
                    isHiddenByPinAnimation -> 0f
                    else -> 1f
                }
            },
    ) {
        ConversationListItemRow(
            item = item,
            modifier = Modifier.conversationRowHorizontalPadding(horizontalInsets),
            onClick = { callbacks.onClick(item.conversationId) },
            onLongClick = { callbacks.onLongClick(item.conversationId) },
            isSelectionMode = isSelectionMode,
            onAvatarMessageClick = {
                callbacks.onAvatarMessageClick(item.conversationId)
            },
            onAvatarCallClick = {
                if (destination != null) {
                    callbacks.onAvatarCallClick(destination)
                }
            }.takeIf { item.avatar.canCall },
            onAvatarContactClick = {
                callbacks.onAvatarContactClick(item)
            }.takeIf { item.avatar.canShowContact },
            onAvatarInfoClick = {
                callbacks.onAvatarInfoClick(item.conversationId)
            },
        )
    }
}

internal fun Modifier.conversationRowHorizontalPadding(horizontalInsets: PaddingValues): Modifier {
    return padding(horizontalInsets)
        .padding(horizontal = ListContentPadding)
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
