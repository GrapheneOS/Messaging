package com.android.messaging.ui.common.components.selection

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val LOAD_MORE_THRESHOLD = 10

@Composable
internal fun SelectionListContent(
    canLoadMore: Boolean,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    loadMoreItemCount: Int,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    isFloatingActionVisible: Boolean = false,
    floatingActionEnterTransition: EnterTransition = EnterTransition.None,
    floatingActionExitTransition: ExitTransition = ExitTransition.None,
    floatingActionContent: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()

    val animatedListBottomPadding by animateDpAsState(
        targetValue = when {
            isFloatingActionVisible -> 100.dp
            else -> 16.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "selectionListBottomPadding",
    )

    SelectionListLoadMoreEffect(
        listState = listState,
        canLoadMore = canLoadMore,
        isLoading = isLoading,
        isLoadingMore = isLoadingMore,
        loadMoreItemCount = loadMoreItemCount,
        onLoadMore = onLoadMore,
    )

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = animatedListBottomPadding),
            content = content,
        )

        SelectionAnimatedVisibility(
            modifier = Modifier.align(alignment = Alignment.BottomEnd),
            visible = isFloatingActionVisible,
            enter = floatingActionEnterTransition,
            exit = floatingActionExitTransition,
        ) {
            floatingActionContent()
        }
    }
}

@Composable
private fun SelectionListLoadMoreEffect(
    listState: LazyListState,
    canLoadMore: Boolean,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    loadMoreItemCount: Int,
    onLoadMore: () -> Unit,
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(
        listState,
        canLoadMore,
        isLoading,
        isLoadingMore,
        loadMoreItemCount,
    ) {
        snapshotFlow {
            val lastVisibleIndex = listState
                .layoutInfo
                .visibleItemsInfo
                .lastOrNull()
                ?.index
                ?: -1

            lastVisibleIndex >= loadMoreItemCount - LOAD_MORE_THRESHOLD
        }.collect { isNearEnd ->
            if (
                shouldRequestSelectionListLoadMore(
                    isNearEnd = isNearEnd,
                    canLoadMore = canLoadMore,
                    isLoading = isLoading,
                    isLoadingMore = isLoadingMore,
                )
            ) {
                currentOnLoadMore()
            }
        }
    }
}

private fun shouldRequestSelectionListLoadMore(
    isNearEnd: Boolean,
    canLoadMore: Boolean,
    isLoading: Boolean,
    isLoadingMore: Boolean,
): Boolean {
    return isNearEnd &&
        canLoadMore &&
        !isLoading &&
        !isLoadingMore
}
