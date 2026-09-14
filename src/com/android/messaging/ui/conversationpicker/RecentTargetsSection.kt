package com.android.messaging.ui.conversationpicker

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.selection.SelectionListItemTokens
import com.android.messaging.ui.conversationpicker.common.SectionHeader
import com.android.messaging.ui.conversationpicker.common.TargetItem
import com.android.messaging.ui.conversationpicker.model.ConversationPickerAction as Action
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

private val PermissionCardTopPadding = 8.dp
private val RecentTargetItemBottomPadding = 2.dp

@Composable
internal fun RecentTargetsSection(
    recentTargets: ImmutableList<TargetUiState>,
    selectedIds: ImmutableSet<String>,
    inSelectionMode: Boolean,
    allowMultiSelect: Boolean,
    canLoadMoreRecent: Boolean,
    canCollapseRecent: Boolean,
    hasContactsPermission: Boolean,
    onAction: (Action) -> Unit,
    onGrantContactsPermission: () -> Unit,
    @StringRes recentConversationsTitle: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (recentTargets.isNotEmpty()) {
            SectionHeader(
                text = stringResource(id = recentConversationsTitle),
            )
        }

        recentTargets.forEachIndexed { index, target ->
            TargetItem(
                modifier = Modifier.padding(
                    bottom = when {
                        index == recentTargets.lastIndex -> 0.dp
                        else -> RecentTargetItemBottomPadding
                    },
                ),
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
                    onAction(Action.SelectionToggled(target))
                }.takeIf { allowMultiSelect },
                shape = SelectionListItemTokens.shape(
                    index = index,
                    totalCount = recentTargets.size,
                ),
            )
        }

        if (canLoadMoreRecent || canCollapseRecent) {
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

        if (!hasContactsPermission) {
            ContactsPermissionCard(
                onGrant = onGrantContactsPermission,
                modifier = Modifier.padding(top = PermissionCardTopPadding),
            )
        }
    }
}
