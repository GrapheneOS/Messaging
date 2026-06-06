package com.android.messaging.ui.shareintent.screen

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private const val LOAD_MORE_PREFETCH_DISTANCE = 10

@Composable
internal fun LoadMoreContactsOnScroll(
    listState: LazyListState,
    enabled: Boolean,
    onLoadMore: () -> Unit,
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState, enabled) {
        if (!enabled) {
            return@LaunchedEffect
        }

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItemsCount = layoutInfo.totalItemsCount
            val prefetchThreshold = totalItemsCount - LOAD_MORE_PREFETCH_DISTANCE

            totalItemsCount > 0 && lastVisibleIndex >= prefetchThreshold
        }
            .distinctUntilChanged()
            .collect { isNearEnd ->
                if (isNearEnd) {
                    currentOnLoadMore()
                }
            }
    }
}
