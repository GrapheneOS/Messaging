package com.android.messaging.ui.recipientselection.component

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.common.components.PrimaryActionButton
import com.android.messaging.ui.common.components.selection.SelectionListContent
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.recipientselection.model.section.RecipientContactListEntry
import com.android.messaging.ui.recipientselection.model.selection.OnRecipientDestinationAction
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionRowDecorators
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

private const val RECIPIENT_CONTACT_CONTENT_TYPE = "recipient_contact"
private const val RECIPIENT_SECTION_HEADER_CONTENT_TYPE = "recipient_section_header"

@Composable
internal fun RecipientSelectionContactsContent(
    uiState: RecipientSelectionContentUiState,
    rowDecorators: RecipientSelectionRowDecorators,
    onLoadMore: () -> Unit,
    onPrimaryActionClick: () -> Unit,
    onRecipientDestinationClick: OnRecipientDestinationAction,
    onRecipientDestinationLongClick: OnRecipientDestinationAction?,
    @StringRes emptyStateText: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    topListContent: (@Composable () -> Unit)? = null,
) {
    val primaryAction = uiState.primaryAction
    val pickerUiState = uiState.picker

    val selectedDestinations = remember(uiState.selectedRecipients) {
        uiState.selectedRecipients
            .map { recipient -> recipient.destination }
            .toImmutableSet()
    }

    val showSectionHeaders = pickerUiState.query.isBlank()
    val contactEntries = remember(pickerUiState.items, showSectionHeaders) {
        recipientContactListEntries(
            items = pickerUiState.items,
            showSectionHeaders = showSectionHeaders,
        )
    }

    SelectionListContent(
        modifier = modifier,
        canLoadMore = pickerUiState.canLoadMore,
        isLoading = pickerUiState.isLoading,
        isLoadingMore = pickerUiState.isLoadingMore,
        loadMoreItemCount = pickerUiState.items.size,
        onLoadMore = onLoadMore,
        contentPadding = contentPadding,
        isFloatingActionVisible = primaryAction != null,
        floatingActionEnterTransition = recipientSelectionPrimaryActionEnterTransition(),
        floatingActionExitTransition = recipientSelectionPrimaryActionExitTransition(),
        floatingActionContent = {
            PrimaryActionButton(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(end = 8.dp, bottom = 8.dp),
                enabled = primaryAction?.isEnabled ?: false,
                isLoading = primaryAction?.isLoading ?: false,
                text = primaryAction?.text.orEmpty(),
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
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
            entries = contactEntries,
            selectedDestinations = selectedDestinations,
            rowDecorators = rowDecorators,
            onRecipientDestinationClick = onRecipientDestinationClick,
            onRecipientDestinationLongClick = onRecipientDestinationLongClick,
            emptyStateText = emptyStateText,
        )
    }
}

private fun LazyListScope.recipientSelectionContactItems(
    uiState: RecipientSelectionContentUiState,
    entries: ImmutableList<RecipientContactListEntry>,
    selectedDestinations: ImmutableSet<String>,
    rowDecorators: RecipientSelectionRowDecorators,
    onRecipientDestinationClick: OnRecipientDestinationAction,
    onRecipientDestinationLongClick: OnRecipientDestinationAction?,
    @StringRes emptyStateText: Int,
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
                RecipientSelectionEmptyState(text = emptyStateText)
            }
        }

        else -> {
            items(
                items = entries,
                key = { entry -> entry.key },
                contentType = { entry -> entry.contentType() },
            ) { entry ->
                when (entry) {
                    is RecipientContactListEntry.Header -> {
                        RecipientContactSectionHeader(entry.label)
                    }

                    is RecipientContactListEntry.Row -> {
                        RecipientSelectionContactItem(
                            entry = entry,
                            uiState = uiState,
                            selectedDestinations = selectedDestinations,
                            rowDecorators = rowDecorators,
                            onRecipientDestinationClick = onRecipientDestinationClick,
                            onRecipientDestinationLongClick = onRecipientDestinationLongClick,
                        )
                    }
                }
            }
        }
    }

    if (pickerUiState.isLoadingMore) {
        item {
            RecipientSelectionLoadingMoreState()
        }
    }
}

private fun RecipientContactListEntry.contentType(): String {
    return when (this) {
        is RecipientContactListEntry.Header -> RECIPIENT_SECTION_HEADER_CONTENT_TYPE
        is RecipientContactListEntry.Row -> RECIPIENT_CONTACT_CONTENT_TYPE
    }
}

@Composable
private fun RecipientContactSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = rowHorizontalPadding,
                top = 4.dp,
                bottom = 4.dp,
            ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun RecipientSelectionContactItem(
    entry: RecipientContactListEntry.Row,
    uiState: RecipientSelectionContentUiState,
    selectedDestinations: ImmutableSet<String>,
    rowDecorators: RecipientSelectionRowDecorators,
    onRecipientDestinationClick: OnRecipientDestinationAction,
    onRecipientDestinationLongClick: OnRecipientDestinationAction?,
) {
    val item = entry.item
    val bottomPadding = when {
        entry.positionInSection == entry.sectionSize - 1 -> 0.dp
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
            index = entry.positionInSection,
            totalCount = entry.sectionSize,
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
private fun RecipientSelectionEmptyState(
    @StringRes text: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 24.dp),
        text = stringResource(id = text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
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
private fun RecipientSelectionContactsContentSectionedPreview() {
    MessagingPreviewColumn {
        PreviewRecipientSelectionContactsContent(
            modifier = Modifier.height(height = 480.dp),
            uiState = previewRecipientSelectionContactsSectionedState(),
            onRecipientDestinationLongClick = null,
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
