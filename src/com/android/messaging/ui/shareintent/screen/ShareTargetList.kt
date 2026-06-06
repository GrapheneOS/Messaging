package com.android.messaging.ui.shareintent.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.shareintent.common.ItemDividerHorizontalInset
import com.android.messaging.ui.shareintent.common.ScreenContentPadding
import com.android.messaging.ui.shareintent.common.ShareTargetItem
import com.android.messaging.ui.shareintent.screen.model.ShareContactSection
import com.android.messaging.ui.shareintent.screen.model.ShareIntentAction as Action
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

private val SectionHeaderHorizontalPadding = 16.dp
private val SectionHeaderVerticalPadding = 8.dp
private val ItemDividerThickness = 1.dp

@Composable
internal fun ShareTargetList(
    recentTargets: ImmutableList<ShareTargetUiState>,
    contactSections: ImmutableList<ShareContactSection>,
    selectedIds: ImmutableSet<String>,
    inSelectionMode: Boolean,
    allowMultiSelect: Boolean,
    canLoadMoreRecent: Boolean,
    canCollapseRecent: Boolean,
    hasContactsPermission: Boolean,
    canLoadMoreContacts: Boolean,
    onAction: (Action) -> Unit,
    onGrantContactsPermission: () -> Unit,
    bottomPadding: Dp,
) {
    val listState = rememberLazyListState()

    LoadMoreContactsOnScroll(
        listState = listState,
        enabled = canLoadMoreContacts,
        onLoadMore = { onAction(Action.LoadMoreContacts) },
    )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = ScreenContentPadding,
            bottom = ScreenContentPadding + bottomPadding,
            start = ScreenContentPadding,
            end = ScreenContentPadding,
        ),
    ) {
        recentTargetsSection(
            recentTargets = recentTargets,
            selectedIds = selectedIds,
            inSelectionMode = inSelectionMode,
            allowMultiSelect = allowMultiSelect,
            canLoadMoreRecent = canLoadMoreRecent,
            canCollapseRecent = canCollapseRecent,
            onAction = onAction,
        )

        contactsSection(
            contactSections = contactSections,
            selectedIds = selectedIds,
            inSelectionMode = inSelectionMode,
            allowMultiSelect = allowMultiSelect,
            hasContactsPermission = hasContactsPermission,
            onAction = onAction,
            onGrantContactsPermission = onGrantContactsPermission,
        )
    }
}

private fun LazyListScope.recentTargetsSection(
    recentTargets: ImmutableList<ShareTargetUiState>,
    selectedIds: ImmutableSet<String>,
    inSelectionMode: Boolean,
    allowMultiSelect: Boolean,
    canLoadMoreRecent: Boolean,
    canCollapseRecent: Boolean,
    onAction: (Action) -> Unit,
) {
    if (recentTargets.isNotEmpty()) {
        item(key = "recent_header") {
            ShareSectionHeader(
                text = stringResource(R.string.share_recent_conversations_title),
                modifier = Modifier.animateItem(),
            )
        }
    }

    itemsIndexed(
        items = recentTargets,
        key = { _, target -> target.key },
    ) { index, target ->
        Column(modifier = Modifier.animateItem()) {
            if (index > 0) {
                ItemDivider()
            }

            ShareTargetRow(
                target = target,
                isSelected = target.selectionId in selectedIds,
                inSelectionMode = inSelectionMode,
                allowMultiSelect = allowMultiSelect,
                onAction = onAction,
            )
        }
    }

    if (canLoadMoreRecent || canCollapseRecent) {
        item(key = "load_more_recent") {
            TextButton(
                onClick = {
                    val action = when {
                        canLoadMoreRecent -> Action.LoadMoreRecent
                        else -> Action.CollapseRecent
                    }
                    onAction(action)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                val textRes = when {
                    canLoadMoreRecent -> R.string.share_recent_show_more_action
                    else -> R.string.share_recent_show_less_action
                }

                Text(text = stringResource(textRes))
            }
        }
    }
}

private fun LazyListScope.contactsSection(
    contactSections: ImmutableList<ShareContactSection>,
    selectedIds: ImmutableSet<String>,
    inSelectionMode: Boolean,
    allowMultiSelect: Boolean,
    hasContactsPermission: Boolean,
    onAction: (Action) -> Unit,
    onGrantContactsPermission: () -> Unit,
) {
    when {
        !hasContactsPermission -> {
            item(key = "contacts_permission") {
                ContactsPermissionCard(
                    onGrant = onGrantContactsPermission,
                    modifier = Modifier.animateItem(),
                )
            }
        }

        contactSections.isNotEmpty() -> {
            contactSections.forEach { section ->
                item(key = "contacts_header_${section.label}") {
                    ShareSectionHeader(
                        text = section.label,
                        modifier = Modifier.animateItem(),
                    )
                }

                itemsIndexed(
                    items = section.targets,
                    key = { _, target -> target.key },
                ) { index, target ->
                    Column(modifier = Modifier.animateItem()) {
                        if (index > 0) {
                            ItemDivider()
                        }

                        ShareTargetRow(
                            target = target,
                            isSelected = target.selectionId in selectedIds,
                            inSelectionMode = inSelectionMode,
                            allowMultiSelect = allowMultiSelect,
                            onAction = onAction,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareTargetRow(
    target: ShareTargetUiState,
    isSelected: Boolean,
    inSelectionMode: Boolean,
    allowMultiSelect: Boolean,
    onAction: (Action) -> Unit,
) {
    ShareTargetItem(
        target = target,
        isSelected = isSelected,
        onClick = {
            val action = when {
                inSelectionMode -> Action.SelectionToggled(target)
                else -> Action.TargetClicked(target)
            }
            onAction(action)
        },
        onLongClick = {
            onAction(Action.SelectionToggled(target))
        }.takeIf { allowMultiSelect },
    )
}

@Composable
private fun ShareSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = SectionHeaderHorizontalPadding,
                vertical = SectionHeaderVerticalPadding,
            ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(
            horizontal = ItemDividerHorizontalInset,
            vertical = ItemDividerThickness,
        ),
        thickness = ItemDividerThickness,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}
