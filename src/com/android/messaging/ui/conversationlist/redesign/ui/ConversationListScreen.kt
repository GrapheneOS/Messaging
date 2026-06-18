package com.android.messaging.ui.conversationlist.redesign.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.ui.conversationlist.redesign.ConversationListEffectHandler
import com.android.messaging.ui.conversationlist.redesign.ConversationListScreenModel
import com.android.messaging.ui.conversationlist.redesign.ConversationListViewModel
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListEffect as Effect
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListUiState as State
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val FabSpacing = 16.dp
private val FabBottomReserve = 72.dp
private val FabShape = RoundedCornerShape(16.dp)
private val ContentCornerShape = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 28.dp,
)

@Composable
internal fun ConversationListScreen(
    effectHandler: ConversationListEffectHandler,
    modifier: Modifier = Modifier,
    screenModel: ConversationListScreenModel = viewModel<ConversationListViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingAddContactDestination by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf(false) }
    var pendingBlockDestination by rememberSaveable { mutableStateOf<String?>(null) }

    ConversationListEffects(
        screenModel = screenModel,
        effectHandler = effectHandler,
        listState = listState,
        snackbarHostState = snackbarHostState,
        onConfirmAddContact = { pendingAddContactDestination = it },
        onConfirmBlock = { pendingBlockDestination = it },
    )

    ConversationListScaffold(
        uiState = uiState,
        listState = listState,
        snackbarHostState = snackbarHostState,
        onAction = screenModel::onAction,
        onDeleteClick = { pendingDelete = true },
        onScrollToTop = { screenModel.onAction(Action.ScrollUpClicked) },
        modifier = modifier,
    )

    ConversationListDialogs(
        selectedCount = uiState.selection.selectedConversations.size,
        addContactDestination = pendingAddContactDestination,
        isDeleteVisible = pendingDelete,
        blockDestination = pendingBlockDestination,
        onAction = screenModel::onAction,
        onDismissAddContact = { pendingAddContactDestination = null },
        onDismissDelete = { pendingDelete = false },
        onDismissBlock = { pendingBlockDestination = null },
    )
}

@Composable
private fun ConversationListEffects(
    screenModel: ConversationListScreenModel,
    effectHandler: ConversationListEffectHandler,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onConfirmAddContact: (String) -> Unit,
    onConfirmBlock: (String) -> Unit,
) {
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.snack_bar_undo)
    val snackbarScope = rememberCoroutineScope()

    val currentContext by rememberUpdatedState(context)
    val currentEffectHandler by rememberUpdatedState(effectHandler)
    val currentUndoLabel by rememberUpdatedState(undoLabel)
    val currentOnConfirmAddContact by rememberUpdatedState(onConfirmAddContact)
    val currentOnConfirmBlock by rememberUpdatedState(onConfirmBlock)

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        screenModel.onAction(Action.ScreenResumed)
    }

    LaunchedEffect(screenModel) {
        screenModel.effects.collect { effect ->
            when (effect) {
                is Effect.ConfirmAddContact -> {
                    currentOnConfirmAddContact(effect.destination)
                }

                is Effect.ConfirmBlock -> {
                    currentOnConfirmBlock(effect.destination)
                }

                is Effect.ConversationsArchived -> {
                    snackbarScope.launchArchivedSnackbar(
                        snackbarHostState = snackbarHostState,
                        context = currentContext,
                        undoLabel = currentUndoLabel,
                        effect = effect,
                        onAction = screenModel::onAction,
                    )
                }

                is Effect.ConversationBlocked -> {
                    snackbarScope.launchBlockedSnackbar(
                        snackbarHostState = snackbarHostState,
                        context = currentContext,
                        undoLabel = currentUndoLabel,
                        effect = effect,
                        onAction = screenModel::onAction,
                    )
                }

                Effect.ScrollToTop -> {
                    listState.animateScrollToItem(index = 0)
                }

                else -> currentEffectHandler.handle(effect)
            }
        }
    }
}

private fun CoroutineScope.launchArchivedSnackbar(
    snackbarHostState: SnackbarHostState,
    context: Context,
    undoLabel: String,
    effect: Effect.ConversationsArchived,
    onAction: (Action) -> Unit,
) {
    val messageResId = when {
        effect.isArchived -> R.string.archived_toast_message
        else -> R.string.unarchived_toast_message
    }

    launch {
        showUndoableSnackbar(
            snackbarHostState = snackbarHostState,
            message = context.getString(messageResId, effect.count),
            undoLabel = undoLabel,
            onUndo = {
                onAction(
                    Action.ArchiveUndoClicked(
                        conversationIds = effect.conversationIds,
                        isArchived = effect.isArchived,
                    ),
                )
            },
        )
    }
}

private fun CoroutineScope.launchBlockedSnackbar(
    snackbarHostState: SnackbarHostState,
    context: Context,
    undoLabel: String,
    effect: Effect.ConversationBlocked,
    onAction: (Action) -> Unit,
) {
    if (!effect.success) {
        return
    }

    launch {
        showUndoableSnackbar(
            snackbarHostState = snackbarHostState,
            message = context.getString(R.string.update_destination_blocked),
            undoLabel = undoLabel,
            onUndo = {
                onAction(
                    Action.BlockUndoClicked(
                        conversationId = effect.conversationId,
                        destination = effect.destination,
                    ),
                )
            },
        )
    }
}

private suspend fun showUndoableSnackbar(
    snackbarHostState: SnackbarHostState,
    message: String,
    undoLabel: String,
    onUndo: () -> Unit,
) {
    val snackbarResult = snackbarHostState.showSnackbar(
        message = message,
        actionLabel = undoLabel,
    )

    if (snackbarResult == SnackbarResult.ActionPerformed) {
        onUndo()
    }
}

@Composable
private fun ConversationListScaffold(
    uiState: State,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onAction: (Action) -> Unit,
    onDeleteClick: () -> Unit,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelectionMode = uiState.selection.isActive
    val backdropColor = conversationListBackdropColor(isSelectionMode)

    BackHandler(enabled = isSelectionMode) {
        onAction(Action.SelectionCleared)
    }

    ConversationListScrollReporter(
        listState = listState,
        onAction = onAction,
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            ConversationListTopBar(
                uiState = uiState,
                isSelectionMode = isSelectionMode,
                onAction = onAction,
                onDeleteClick = onDeleteClick,
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
                .background(backdropColor)
                .clip(ContentCornerShape)
                .background(MaterialTheme.colorScheme.background),
        ) {
            ConversationListContent(
                content = uiState.content,
                listState = listState,
                onAction = onAction,
                contentPadding = contentPadding,
                bottomReserve = FabBottomReserve,
            )

            ScrollToTopFab(
                visible = uiState.isScrollUpVisible,
                onClick = onScrollToTop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = FabSpacing),
            )

            StartChatFab(
                visible = !isSelectionMode,
                expanded = !uiState.isScrollUpVisible,
                onClick = { onAction(Action.StartChatClicked) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(FabSpacing),
            )
        }
    }
}

@Composable
private fun conversationListBackdropColor(isSelectionMode: Boolean): Color {
    return when {
        isSelectionMode -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
}

@Composable
private fun ConversationListTopBar(
    uiState: State,
    isSelectionMode: Boolean,
    onAction: (Action) -> Unit,
    onDeleteClick: () -> Unit,
) {
    when {
        isSelectionMode -> {
            ConversationListSelectionTopAppBar(
                selectedCount = uiState.selection.selectedConversations.size,
                actions = uiState.selection.actions,
                onAction = onAction,
                onDeleteClick = onDeleteClick,
            )
        }

        else -> {
            ConversationListTopAppBar(
                hasBlockedParticipants = uiState.hasBlockedParticipants,
                isDebugEnabled = uiState.isDebugEnabled,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun ConversationListScrollReporter(
    listState: LazyListState,
    onAction: (Action) -> Unit,
) {
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
        }
            .distinctUntilChanged()
            .collect { isAtTop ->
                onAction(Action.NewestConversationVisibilityChanged(isVisible = isAtTop))
            }
    }
}

@Composable
private fun ScrollToTopFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowUpward,
                contentDescription = stringResource(R.string.conversation_list_scroll_to_top),
            )
        }
    }
}

@Composable
private fun StartChatFab(
    visible: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            expanded = expanded,
            shape = FabShape,
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Chat,
                    contentDescription = stringResource(R.string.conversation_list_start_chat),
                )
            },
            text = {
                Text(text = stringResource(R.string.conversation_list_start_chat))
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationListScaffoldItemsPreview() {
    MessagingPreviewTheme {
        ConversationListScaffold(
            uiState = State(
                content = ConversationListContentUiState.Items(
                    items = previewConversationListItems(),
                ),
            ),
            listState = rememberLazyListState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onDeleteClick = {},
            onScrollToTop = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationListScaffoldEmptyPreview() {
    MessagingPreviewTheme {
        ConversationListScaffold(
            uiState = State(
                content = ConversationListContentUiState.Empty,
                isDebugEnabled = true,
            ),
            listState = rememberLazyListState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onDeleteClick = {},
            onScrollToTop = {},
        )
    }
}
