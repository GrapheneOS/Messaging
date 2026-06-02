package com.android.messaging.ui.shareintent.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.shareintent.common.ItemDividerHorizontalInset
import com.android.messaging.ui.shareintent.common.NewMessageItem
import com.android.messaging.ui.shareintent.common.ScreenContentPadding
import com.android.messaging.ui.shareintent.common.SelectedTargetsBar
import com.android.messaging.ui.shareintent.common.ShareIntentTopAppBar
import com.android.messaging.ui.shareintent.common.ShareTargetItem
import com.android.messaging.ui.shareintent.common.contentSurfaceShape
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import com.android.messaging.ui.shareintent.screen.model.ShareIntentAction as Action
import com.android.messaging.ui.shareintent.screen.model.ShareIntentUiState as State

@Composable
internal fun ShareIntentScreen(
    effectHandler: ShareIntentEffectHandler,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: ShareIntentScreenModel = viewModel<ShareIntentViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    val currentEffectHandler by rememberUpdatedState(effectHandler)
    LaunchedEffect(screenModel) {
        screenModel.effects.collect { effect ->
            currentEffectHandler.handle(effect)
        }
    }

    ShareIntentContent(
        uiState = uiState,
        onAction = screenModel::onAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@Composable
private fun ShareIntentContent(
    uiState: State,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchState = rememberTextFieldState()
    val inSelectionMode = uiState.selectedConversationIds.isNotEmpty()

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .collect { query ->
                onAction(Action.SearchQueryChanged(query))
            }
    }

    BackHandler(enabled = inSelectionMode) {
        onAction(Action.SelectionCleared)
    }

    BackHandler(enabled = uiState.isSearchActive) {
        searchState.clearText()
        onAction(Action.SearchClosed)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                ShareIntentTopAppBar(
                    isSearchActive = uiState.isSearchActive,
                    inSelectionMode = inSelectionMode,
                    selectedCount = uiState.selectedConversationIds.size,
                    searchState = searchState,
                    onNavigateBack = onNavigateBack,
                    onSearchOpen = { onAction(Action.SearchOpened) },
                    onSearchClose = {
                        searchState.clearText()
                        onAction(Action.SearchClosed)
                    },
                    onSelectionClear = { onAction(Action.SelectionCleared) },
                )

                AnimatedVisibility(
                    visible = inSelectionMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    SelectedTargetsBar(
                        targets = uiState.selectedTargets,
                        onRemove = { onAction(Action.SelectionToggled(it)) },
                        onSend = { onAction(Action.SendToSelectedClicked) },
                    )
                }
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
                .clip(MaterialTheme.contentSurfaceShape)
                .background(MaterialTheme.colorScheme.background),
        ) {
            if (!uiState.isLoading) {
                ShareTargetList(
                    targets = uiState.targets,
                    selectedConversationIds = uiState.selectedConversationIds,
                    inSelectionMode = inSelectionMode,
                    showNewMessage = !uiState.isSearchActive && !inSelectionMode,
                    onAction = onAction,
                    bottomPadding = contentPadding.calculateBottomPadding(),
                )
            }
        }
    }
}

@Composable
private fun ShareTargetList(
    targets: List<ShareTargetUiState>,
    selectedConversationIds: Set<String>,
    inSelectionMode: Boolean,
    showNewMessage: Boolean,
    onAction: (Action) -> Unit,
    bottomPadding: Dp,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = ScreenContentPadding,
            bottom = ScreenContentPadding + bottomPadding,
            start = ScreenContentPadding,
            end = ScreenContentPadding,
        ),
    ) {
        if (showNewMessage) {
            item(key = "new_message") {
                NewMessageItem(
                    onClick = { onAction(Action.NewMessageClicked) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        itemsIndexed(
            items = targets,
            key = { _, target -> target.conversationId },
        ) { index, target ->
            Column(modifier = Modifier.animateItem()) {
                if (showNewMessage || index > 0) {
                    ItemDivider()
                }

                ShareTargetItem(
                    target = target,
                    isSelected = target.conversationId in selectedConversationIds,
                    onClick = {
                        val action = when {
                            inSelectionMode -> Action.SelectionToggled(target.conversationId)
                            else -> Action.TargetClicked(target.conversationId)
                        }
                        onAction(action)
                    },
                    onLongClick = {
                        onAction(Action.TargetLongPressed(target.conversationId))
                    },
                )
            }
        }
    }
}

@Composable
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(
            horizontal = ItemDividerHorizontalInset,
            vertical = 1.dp,
        ),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

@Preview
@Composable
private fun ShareIntentContentPreview() {
    AppTheme {
        ShareIntentContent(
            uiState = State(
                isLoading = false,
                targets = persistentListOf(
                    ShareTargetUiState(
                        conversationId = "1",
                        displayName = "Jane Doe",
                        details = "+31 6 1234 5678",
                        avatarUri = null,
                        isGroup = false,
                    ),
                    ShareTargetUiState(
                        conversationId = "2",
                        displayName = "Project group",
                        details = null,
                        avatarUri = null,
                        isGroup = true,
                    ),
                ),
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun ShareIntentSelectionPreview() {
    val targets = persistentListOf(
        ShareTargetUiState(
            conversationId = "1",
            displayName = "Jane Doe",
            details = "+31 6 1234 5678",
            avatarUri = null,
            isGroup = false,
        ),
        ShareTargetUiState(
            conversationId = "2",
            displayName = "Project group",
            details = null,
            avatarUri = null,
            isGroup = true,
        ),
    )

    AppTheme {
        ShareIntentContent(
            uiState = State(
                isLoading = false,
                targets = targets,
                selectedConversationIds = persistentSetOf("1", "2"),
                selectedTargets = targets,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun ShareIntentEmptyPreview() {
    AppTheme {
        ShareIntentContent(
            uiState = State(isLoading = false),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
