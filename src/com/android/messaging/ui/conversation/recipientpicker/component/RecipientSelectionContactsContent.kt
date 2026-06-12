package com.android.messaging.ui.conversation.recipientpicker.component

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.selection.SelectionListContent
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerListItem
import com.android.messaging.ui.conversation.recipientpicker.model.selection.OnRecipientDestinationAction
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionRowDecorators
import com.android.messaging.ui.core.MessagingPreviewColumn
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

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
    val pickerUiState = uiState.picker

    val selectedDestinations = remember(uiState.selectedRecipients) {
        uiState.selectedRecipients
            .map { recipient -> recipient.destination }
            .toImmutableSet()
    }

    SelectionListContent(
        modifier = modifier,
        canLoadMore = pickerUiState.canLoadMore,
        isLoading = pickerUiState.isLoading,
        isLoadingMore = pickerUiState.isLoadingMore,
        loadMoreItemCount = pickerUiState.items.size,
        onLoadMore = onLoadMore,
        isFloatingActionVisible = primaryAction != null,
        floatingActionEnterTransition = recipientSelectionPrimaryActionEnterTransition(),
        floatingActionExitTransition = recipientSelectionPrimaryActionExitTransition(),
        floatingActionContent = {
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
        },
    ) {
        topListContent?.let {
            item {
                topListContent()
            }
        }

        recipientSelectionContactItems(
            uiState = uiState,
            selectedDestinations = selectedDestinations,
            rowDecorators = rowDecorators,
            onRecipientDestinationClick = onRecipientDestinationClick,
            onRecipientDestinationLongClick = onRecipientDestinationLongClick,
        )
    }
}

private fun LazyListScope.recipientSelectionContactItems(
    uiState: RecipientSelectionContentUiState,
    selectedDestinations: ImmutableSet<String>,
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
                    selectedDestinations = selectedDestinations,
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
    selectedDestinations: ImmutableSet<String>,
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
        selectedDestinations = selectedDestinations,
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

@PreviewLightDark
@Composable
private fun RecipientSelectionContactsContentLoadedPreview() {
    MessagingPreviewColumn {
        PreviewRecipientSelectionContactsContent(
            modifier = Modifier.height(height = 420.dp),
            uiState = previewRecipientSelectionContactsLoadedState(),
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactsContentLoadingPreview() {
    MessagingPreviewColumn {
        PreviewRecipientSelectionContactsContent(
            modifier = Modifier.height(height = 260.dp),
            uiState = previewRecipientSelectionContactsLoadingState(),
            onRecipientDestinationLongClick = null,
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactsContentEmptyPreview() {
    MessagingPreviewColumn {
        PreviewRecipientSelectionContactsContent(
            modifier = Modifier.height(height = 260.dp),
            uiState = previewRecipientSelectionContactsEmptyState(),
            onRecipientDestinationLongClick = null,
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactsContentLoadingMorePreview() {
    MessagingPreviewColumn {
        PreviewRecipientSelectionContactsContent(
            modifier = Modifier.height(height = 420.dp),
            uiState = previewRecipientSelectionContactsLoadingMoreState(),
            loadingDestination = RECIPIENT_ROW_PREVIEW_EMAIL_DESTINATION,
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactsContentPrimaryActionDisabledPreview() {
    MessagingPreviewColumn {
        PreviewRecipientSelectionContactsContent(
            modifier = Modifier.height(height = 420.dp),
            uiState = previewRecipientSelectionContactsPrimaryActionDisabledState(),
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactsContentPrimaryActionLoadingPreview() {
    MessagingPreviewColumn {
        PreviewRecipientSelectionContactsContent(
            modifier = Modifier.height(height = 420.dp),
            uiState = previewRecipientSelectionContactsPrimaryActionLoadingState(),
            loadingDestination = RECIPIENT_ROW_PREVIEW_SECONDARY_DESTINATION,
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactsContentNoPrimaryActionPreview() {
    MessagingPreviewColumn {
        PreviewRecipientSelectionContactsContent(
            modifier = Modifier.height(height = 360.dp),
            uiState = previewRecipientSelectionContactsNoPrimaryActionState(),
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactsContentTopListContentPreview() {
    MessagingPreviewColumn {
        PreviewRecipientSelectionContactsContent(
            modifier = Modifier.height(height = 420.dp),
            uiState = previewRecipientSelectionContactsTopContentState(),
            topListContent = {
                PreviewRecipientSelectionContactsTopListContent()
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContactsContentLongTextPreview() {
    MessagingPreviewColumn {
        PreviewRecipientSelectionContactsContent(
            modifier = Modifier.height(height = 420.dp),
            uiState = previewRecipientSelectionContactsLongTextState(),
            loadingDestination = RECIPIENT_ROW_PREVIEW_LONG_EMAIL_DESTINATION,
        )
    }
}
