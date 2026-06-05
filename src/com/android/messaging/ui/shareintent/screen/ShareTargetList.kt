package com.android.messaging.ui.shareintent.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.shareintent.common.ItemDividerHorizontalInset
import com.android.messaging.ui.shareintent.common.ScreenContentPadding
import com.android.messaging.ui.shareintent.common.ShareTargetItem
import com.android.messaging.ui.shareintent.screen.model.ShareIntentAction as Action
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState

private val SectionHeaderHorizontalPadding = 16.dp
private val SectionHeaderVerticalPadding = 8.dp
private val ContactsPermissionCardVerticalMargin = 8.dp
private val ContactsPermissionCardCornerRadius = 20.dp
private val ContactsPermissionCardPadding = 20.dp
private val ContactsPermissionContentSpacing = 8.dp
private val ContactsPermissionIconSize = 32.dp
private val ContactsPermissionButtonTopPadding = 4.dp
private val ItemDividerThickness = 1.dp
private const val LOAD_MORE_PREFETCH_DISTANCE = 10
private const val CONTACTS_NON_LETTER_SECTION_LABEL = "#"

@Composable
internal fun ShareTargetList(
    recentTargets: List<ShareTargetUiState>,
    contactTargets: List<ShareTargetUiState>,
    selectedIds: Set<String>,
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
            contactTargets = contactTargets,
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
    recentTargets: List<ShareTargetUiState>,
    selectedIds: Set<String>,
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
                selectedIds = selectedIds,
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
    contactTargets: List<ShareTargetUiState>,
    selectedIds: Set<String>,
    inSelectionMode: Boolean,
    allowMultiSelect: Boolean,
    hasContactsPermission: Boolean,
    onAction: (Action) -> Unit,
    onGrantContactsPermission: () -> Unit,
) {
    when {
        !hasContactsPermission -> {
            item(key = "contacts_permission") {
                ContactsPermissionPrompt(
                    onGrant = onGrantContactsPermission,
                    modifier = Modifier.animateItem(),
                )
            }
        }

        contactTargets.isNotEmpty() -> {
            var previousSectionLabel: String? = null

            contactTargets.forEach { target ->
                val sectionLabel = contactSectionLabel(displayName = target.displayName)
                val isNewSection = sectionLabel != previousSectionLabel
                previousSectionLabel = sectionLabel

                if (isNewSection) {
                    item(key = "contacts_header_$sectionLabel") {
                        ShareSectionHeader(
                            text = sectionLabel,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                item(key = target.key) {
                    Column(modifier = Modifier.animateItem()) {
                        if (!isNewSection) {
                            ItemDivider()
                        }

                        ShareTargetRow(
                            target = target,
                            selectedIds = selectedIds,
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

private fun contactSectionLabel(displayName: String): String {
    val firstCharacter = displayName.trim().firstOrNull()?.uppercaseChar()

    return when {
        firstCharacter?.isLetter() == true -> firstCharacter.toString()
        else -> CONTACTS_NON_LETTER_SECTION_LABEL
    }
}

@Composable
private fun ShareTargetRow(
    target: ShareTargetUiState,
    selectedIds: Set<String>,
    inSelectionMode: Boolean,
    allowMultiSelect: Boolean,
    onAction: (Action) -> Unit,
) {
    ShareTargetItem(
        target = target,
        isSelected = target.selectionId in selectedIds,
        onClick = {
            val action = when {
                inSelectionMode -> Action.SelectionToggled(target)
                else -> Action.TargetClicked(target)
            }
            onAction(action)
        },
        onLongClick = {
            onAction(Action.TargetLongPressed(target))
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
private fun ContactsPermissionPrompt(
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ItemDividerHorizontalInset,
                vertical = ContactsPermissionCardVerticalMargin,
            ),
        shape = RoundedCornerShape(ContactsPermissionCardCornerRadius),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(ContactsPermissionCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ContactsPermissionContentSpacing),
        ) {
            Icon(
                imageVector = Icons.Rounded.Contacts,
                contentDescription = null,
                modifier = Modifier.size(ContactsPermissionIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = stringResource(R.string.share_contacts_permission_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.share_contacts_permission_rationale),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            Button(
                onClick = onGrant,
                modifier = Modifier.padding(top = ContactsPermissionButtonTopPadding),
            ) {
                Text(text = stringResource(R.string.share_contacts_permission_action))
            }
        }
    }
}

@Composable
private fun LoadMoreContactsOnScroll(
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
            lastVisibleIndex to layoutInfo.totalItemsCount
        }.collect { (lastVisibleIndex, totalItemsCount) ->
            if (totalItemsCount > 0 &&
                lastVisibleIndex >= totalItemsCount - LOAD_MORE_PREFETCH_DISTANCE
            ) {
                currentOnLoadMore()
            }
        }
    }
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
