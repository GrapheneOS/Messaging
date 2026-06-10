package com.android.messaging.ui.conversationlist.redesign.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val FabSpacing = 16.dp
private val StartChatButtonHeight = 56.dp
private val StartChatIconSpacing = 8.dp

@Composable
internal fun ConversationListScreen(
    effectHandler: ConversationListEffectHandler,
    isDebugEnabled: Boolean,
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
        isDebugEnabled = isDebugEnabled,
        listState = listState,
        snackbarHostState = snackbarHostState,
        onAction = screenModel::onAction,
        onDeleteClick = { pendingDelete = true },
        onScrollToTop = { screenModel.onAction(Action.ScrollUpClicked) },
        modifier = modifier,
    )

    pendingAddContactDestination?.let { destination ->
        ConversationListAddContactDialog(
            destination = destination,
            onConfirm = {
                pendingAddContactDestination = null
                screenModel.onAction(Action.AddContactConfirmed(destination))
            },
            onDismiss = { pendingAddContactDestination = null },
        )
    }

    if (pendingDelete) {
        ConversationListDeleteDialog(
            selectedCount = uiState.selection.selectedConversations.size,
            onConfirm = {
                pendingDelete = false
                screenModel.onAction(Action.DeleteConfirmed)
            },
            onDismiss = { pendingDelete = false },
        )
    }

    pendingBlockDestination?.let { destination ->
        ConversationListBlockDialog(
            destination = destination,
            onConfirm = {
                pendingBlockDestination = null
                screenModel.onAction(Action.BlockConfirmed)
            },
            onDismiss = { pendingBlockDestination = null },
        )
    }
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
                    snackbarScope.launch {
                        showArchivedSnackbar(
                            snackbarHostState = snackbarHostState,
                            message = currentContext.getString(
                                archivedSnackbarMessageResId(isArchived = effect.isArchived),
                                effect.count,
                            ),
                            undoLabel = currentUndoLabel,
                            onUndo = {
                                screenModel.onAction(
                                    Action.ArchiveUndoClicked(
                                        conversationIds = effect.conversationIds,
                                        isArchived = effect.isArchived,
                                    ),
                                )
                            },
                        )
                    }
                }

                Effect.ScrollToTop -> {
                    listState.animateScrollToItem(index = 0)
                }

                else -> currentEffectHandler.handle(effect)
            }
        }
    }
}

private fun archivedSnackbarMessageResId(isArchived: Boolean): Int {
    return when {
        isArchived -> R.string.archived_toast_message
        else -> R.string.unarchived_toast_message
    }
}

private suspend fun showArchivedSnackbar(
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
    isDebugEnabled: Boolean,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onAction: (Action) -> Unit,
    onDeleteClick: () -> Unit,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelectionMode = uiState.selection.isActive

    BackHandler(enabled = isSelectionMode) {
        onAction(Action.SelectionCleared)
    }

    ConversationListScrollReporter(
        listState = listState,
        onAction = onAction,
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            ConversationListTopBar(
                uiState = uiState,
                isSelectionMode = isSelectionMode,
                isDebugEnabled = isDebugEnabled,
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
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            ConversationListContent(
                content = uiState.content,
                listState = listState,
                onAction = onAction,
                contentPadding = contentPadding,
            )

            ConversationListFabs(
                isStartChatVisible = !isSelectionMode,
                isScrollUpVisible = uiState.isScrollUpVisible,
                onStartChatClick = { onAction(Action.StartChatClicked) },
                onScrollToTopClick = onScrollToTop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(FabSpacing),
            )
        }
    }
}

@Composable
private fun ConversationListTopBar(
    uiState: State,
    isSelectionMode: Boolean,
    isDebugEnabled: Boolean,
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
                isDebugEnabled = isDebugEnabled,
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
private fun ConversationListFabs(
    isStartChatVisible: Boolean,
    isScrollUpVisible: Boolean,
    onStartChatClick: () -> Unit,
    onScrollToTopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(space = FabSpacing),
    ) {
        AnimatedVisibility(
            visible = isScrollUpVisible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            SmallFloatingActionButton(
                onClick = onScrollToTopClick,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = stringResource(R.string.conversation_list_scroll_to_top),
                )
            }
        }

        AnimatedVisibility(
            visible = isStartChatVisible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Button(
                modifier = Modifier.height(StartChatButtonHeight),
                onClick = onStartChatClick,
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.size(StartChatIconSpacing))
                    Text(text = stringResource(R.string.conversation_list_start_chat))
                }
            }
        }
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
            isDebugEnabled = false,
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
            uiState = State(content = ConversationListContentUiState.Empty),
            isDebugEnabled = true,
            listState = rememberLazyListState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onDeleteClick = {},
            onScrollToTop = {},
        )
    }
}
