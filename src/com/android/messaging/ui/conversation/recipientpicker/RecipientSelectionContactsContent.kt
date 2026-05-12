package com.android.messaging.ui.conversation.recipientpicker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.recipientpicker.model.RecipientPickerListItem
import com.android.messaging.ui.conversation.recipientpicker.model.RecipientPickerUiState

private const val CONTACTS_LOAD_MORE_THRESHOLD = 10
private const val RECIPIENT_CONTACT_CONTENT_TYPE = "recipient_contact"

@Composable
internal fun RecipientSelectionContactsContent(
    uiState: RecipientSelectionContentUiState,
    rowDecorators: RecipientSelectionRowDecorators,
    onLoadMore: () -> Unit,
    onPrimaryActionClick: () -> Unit,
    onRecipientDestinationClick: OnRecipientDestinationAction,
    onRecipientDestinationLongClick: OnRecipientDestinationAction?,
    modifier: Modifier = Modifier,
    topListContent: (@Composable () -> Unit)? = null,
) {
    val primaryAction = uiState.primaryAction

    Box(modifier = modifier) {
        RecipientSelectionContactsList(
            uiState = uiState,
            rowDecorators = rowDecorators,
            onLoadMore = onLoadMore,
            onRecipientDestinationClick = onRecipientDestinationClick,
            onRecipientDestinationLongClick = onRecipientDestinationLongClick,
            topListContent = topListContent,
        )

        AnimatedVisibility(
            modifier = Modifier.align(alignment = Alignment.BottomEnd),
            visible = primaryAction != null,
            enter = recipientSelectionPrimaryActionEnterTransition(),
            exit = recipientSelectionPrimaryActionExitTransition(),
        ) {
            RecipientSelectionPrimaryActionButton(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(end = 8.dp, bottom = 8.dp),
                enabled = primaryAction?.isEnabled ?: false,
                isLoading = primaryAction?.isLoading ?: false,
                text = primaryAction?.text.orEmpty(),
                testTag = primaryAction?.testTag,
                onClick = onPrimaryActionClick,
            )
        }
    }
}

@Composable
private fun RecipientSelectionContactsList(
    uiState: RecipientSelectionContentUiState,
    rowDecorators: RecipientSelectionRowDecorators,
    onLoadMore: () -> Unit,
    onRecipientDestinationClick: OnRecipientDestinationAction,
    onRecipientDestinationLongClick: OnRecipientDestinationAction?,
    topListContent: (@Composable () -> Unit)?,
) {
    val pickerUiState = uiState.picker
    val listState = rememberLazyListState()
    val animatedListBottomPadding by animateDpAsState(
        targetValue = when {
            uiState.primaryAction != null -> 100.dp
            else -> 16.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "recipientSelectionListBottomPadding",
    )

    RecipientSelectionLoadMoreEffect(
        listState = listState,
        pickerUiState = pickerUiState,
        onLoadMore = onLoadMore,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = animatedListBottomPadding),
    ) {
        topListContent?.let {
            item {
                topListContent()
            }
        }

        recipientSelectionContactItems(
            uiState = uiState,
            rowDecorators = rowDecorators,
            onRecipientDestinationClick = onRecipientDestinationClick,
            onRecipientDestinationLongClick = onRecipientDestinationLongClick,
        )
    }
}

private fun LazyListScope.recipientSelectionContactItems(
    uiState: RecipientSelectionContentUiState,
    rowDecorators: RecipientSelectionRowDecorators,
    onRecipientDestinationClick: OnRecipientDestinationAction,
    onRecipientDestinationLongClick: OnRecipientDestinationAction?,
) {
    val pickerUiState = uiState.picker

    when {
        pickerUiState.isLoading -> {
            item {
                RecipientSelectionLoadingState()
            }
        }

        pickerUiState.items.isEmpty() -> {
            item {
                RecipientSelectionEmptyState()
            }
        }

        else -> {
            itemsIndexed(
                items = pickerUiState.items,
                key = { _, item -> item.id },
                contentType = { _, _ -> RECIPIENT_CONTACT_CONTENT_TYPE },
            ) { index, item ->
                RecipientSelectionContactItem(
                    item = item,
                    index = index,
                    uiState = uiState,
                    rowDecorators = rowDecorators,
                    onRecipientDestinationClick = onRecipientDestinationClick,
                    onRecipientDestinationLongClick = onRecipientDestinationLongClick,
                )
            }
        }
    }

    if (pickerUiState.isLoadingMore) {
        item {
            RecipientSelectionLoadingMoreState()
        }
    }
}

@Composable
private fun RecipientSelectionContactItem(
    item: RecipientPickerListItem,
    index: Int,
    uiState: RecipientSelectionContentUiState,
    rowDecorators: RecipientSelectionRowDecorators,
    onRecipientDestinationClick: OnRecipientDestinationAction,
    onRecipientDestinationLongClick: OnRecipientDestinationAction?,
) {
    val lastContactIndex = uiState.picker.items.lastIndex
    val bottomPadding = when {
        index == lastContactIndex -> 0.dp
        else -> 2.dp
    }

    RecipientSelectionContactRow(
        modifier = Modifier.padding(bottom = bottomPadding),
        item = item,
        enabled = uiState.primaryAction?.isLoading != true,
        selectedDestinations = uiState.selectedRecipientDestinations,
        onDestinationClick = { destination ->
            onRecipientDestinationClick(item, destination)
        },
        onDestinationLongClick = onRecipientDestinationLongClick?.let { callback ->
            { destination ->
                callback(item, destination)
            }
        },
        rowDecorators = rowDecorators,
        shape = recipientSelectionContactRowShape(
            index = index,
            totalCount = uiState.picker.items.size,
        ),
    )
}

@Composable
private fun RecipientSelectionLoadMoreEffect(
    listState: LazyListState,
    pickerUiState: RecipientPickerUiState,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(
        listState,
        pickerUiState.canLoadMore,
        pickerUiState.isLoading,
        pickerUiState.isLoadingMore,
        pickerUiState.items.size,
    ) {
        snapshotFlow {
            val lastVisibleIndex = listState
                .layoutInfo
                .visibleItemsInfo
                .lastOrNull()
                ?.index
                ?: -1

            lastVisibleIndex >= pickerUiState.items.lastIndex - CONTACTS_LOAD_MORE_THRESHOLD
        }.collect { isNearEnd ->
            if (
                shouldRequestRecipientSelectionLoadMore(
                    isNearEnd = isNearEnd,
                    pickerUiState = pickerUiState,
                )
            ) {
                onLoadMore()
            }
        }
    }
}

private fun shouldRequestRecipientSelectionLoadMore(
    isNearEnd: Boolean,
    pickerUiState: RecipientPickerUiState,
): Boolean {
    return isNearEnd &&
        pickerUiState.canLoadMore &&
        !pickerUiState.isLoading &&
        !pickerUiState.isLoadingMore
}

private fun recipientSelectionPrimaryActionEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = 200),
    ) + slideInVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        initialOffsetY = { fullHeight ->
            fullHeight / 2
        },
    ) + scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        initialScale = 0.9f,
    )
}

private fun recipientSelectionPrimaryActionExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = 150),
    ) + slideOutVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        targetOffsetY = { fullHeight ->
            fullHeight / 2
        },
    ) + scaleOut(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        targetScale = 0.9f,
    )
}

@Composable
private fun RecipientSelectionLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun RecipientSelectionLoadingMoreState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size = 20.dp),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun RecipientSelectionEmptyState() {
    Text(
        modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp),
        text = stringResource(id = R.string.contact_list_empty_text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
