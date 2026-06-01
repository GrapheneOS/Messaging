package com.android.messaging.ui.shareintent.screen

import androidx.activity.compose.BackHandler
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
import com.android.messaging.ui.shareintent.common.ShareIntentTopAppBar
import com.android.messaging.ui.shareintent.common.ShareTargetItem
import com.android.messaging.ui.shareintent.common.contentSurfaceShape
import com.android.messaging.ui.shareintent.screen.model.ShareIntentAction as Action
import com.android.messaging.ui.shareintent.screen.model.ShareIntentUiState as State
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import kotlinx.collections.immutable.persistentListOf

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

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .collect { query ->
                onAction(Action.SearchQueryChanged(query))
            }
    }

    BackHandler(enabled = uiState.isSearchActive) {
        searchState.clearText()
        onAction(Action.SearchClosed)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            ShareIntentTopAppBar(
                isSearchActive = uiState.isSearchActive,
                searchState = searchState,
                onNavigateBack = onNavigateBack,
                onSearchOpen = { onAction(Action.SearchOpened) },
                onSearchClose = {
                    searchState.clearText()
                    onAction(Action.SearchClosed)
                },
            )
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
                    showNewMessage = !uiState.isSearchActive,
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
                    onClick = { onAction(Action.TargetClicked(target.conversationId)) },
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
                    ),
                    ShareTargetUiState(
                        conversationId = "2",
                        displayName = "Project group",
                        details = null,
                        avatarUri = null,
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
private fun ShareIntentEmptyPreview() {
    AppTheme {
        ShareIntentContent(
            uiState = State(isLoading = false),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
