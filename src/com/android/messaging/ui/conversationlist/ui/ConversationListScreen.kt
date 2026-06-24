package com.android.messaging.ui.conversationlist.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.ui.common.components.PrimaryActionButton
import com.android.messaging.ui.common.components.reorder.OverlayReorderAnimation
import com.android.messaging.ui.common.components.reorder.OverlayReorderAnimationController
import com.android.messaging.ui.common.components.reorder.rememberOverlayReorderAnimationController
import com.android.messaging.ui.common.components.snackbar.MessagingSnackbarHost
import com.android.messaging.ui.common.components.snackbar.showActionSnackbar
import com.android.messaging.ui.conversationlist.ConversationListEffectHandler
import com.android.messaging.ui.conversationlist.ConversationListScreenModel
import com.android.messaging.ui.conversationlist.ConversationListViewModel
import com.android.messaging.ui.conversationlist.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListEffect as Effect
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListUiState as State
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val FabSpacing = 16.dp
private val FabBottomReserve = 72.dp
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
    val pinAnimationController = rememberOverlayReorderAnimationController(
        key = ConversationListItemUiModel::conversationId,
        isSettled = { item, anchorToTop -> item.isPinned == anchorToTop },
    )

    var pendingAddContactDestination by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf(false) }
    var pendingBlockConversationId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingBlockDestination by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSnooze by rememberSaveable { mutableStateOf(false) }

    ConversationListEffects(
        effects = screenModel.effects,
        effectHandler = effectHandler,
        listState = listState,
        snackbarHostState = snackbarHostState,
        pinAnimationController = pinAnimationController,
        onAction = screenModel::onAction,
        onConfirmAddContact = { pendingAddContactDestination = it },
        onConfirmBlock = { conversationId, destination ->
            pendingBlockConversationId = conversationId
            pendingBlockDestination = destination
        },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                pinAnimationController.updateContainerBounds(coordinates.boundsInRoot())
            },
    ) {
        ConversationListScaffold(
            uiState = uiState,
            listState = listState,
            snackbarHostState = snackbarHostState,
            pinAnimationController = pinAnimationController,
            onAction = screenModel::onAction,
            onDeleteClick = { pendingDelete = true },
            onSnoozeClick = { pendingSnooze = true },
            onScrollToTop = { screenModel.onAction(Action.ScrollToTopClicked) },
            modifier = Modifier.fillMaxSize(),
        )

        ConversationListPinOverlay(pinAnimationController)
    }

    ConversationListDialogs(
        selectedCount = uiState.selection.selectedCount,
        addContactDestination = pendingAddContactDestination,
        isDeleteVisible = pendingDelete,
        blockConversationId = pendingBlockConversationId,
        blockDestination = pendingBlockDestination,
        isSnoozeVisible = pendingSnooze,
        onAction = screenModel::onAction,
        onDismissAddContact = { pendingAddContactDestination = null },
        onDismissDelete = { pendingDelete = false },
        onDismissBlock = {
            pendingBlockConversationId = null
            pendingBlockDestination = null
        },
        onDismissSnooze = { pendingSnooze = false },
    )
}

@Composable
private fun ConversationListPinOverlay(
    controller: OverlayReorderAnimationController<ConversationListItemUiModel, String>,
) {
    OverlayReorderAnimation(controller = controller) { item ->
        ConversationListItemRow(
            item = item,
            onClick = {},
            onLongClick = {},
        )
    }
}

@Composable
private fun ConversationListEffects(
    effects: Flow<Effect>,
    effectHandler: ConversationListEffectHandler,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    pinAnimationController: OverlayReorderAnimationController<ConversationListItemUiModel, String>,
    onAction: (Action) -> Unit,
    onConfirmAddContact: (String) -> Unit,
    onConfirmBlock: (conversationId: String, destination: String) -> Unit,
) {
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.snack_bar_undo)
    val snackbarScope = rememberCoroutineScope()

    val currentContext by rememberUpdatedState(context)
    val currentEffectHandler by rememberUpdatedState(effectHandler)
    val currentUndoLabel by rememberUpdatedState(undoLabel)
    val currentOnAction by rememberUpdatedState(onAction)
    val currentOnConfirmAddContact by rememberUpdatedState(onConfirmAddContact)
    val currentOnConfirmBlock by rememberUpdatedState(onConfirmBlock)

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        currentOnAction(Action.ScreenResumed)
    }

    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is Effect.ConfirmAddContact -> {
                    currentOnConfirmAddContact(effect.destination)
                }

                is Effect.ConfirmBlock -> {
                    currentOnConfirmBlock(
                        effect.conversationId,
                        effect.destination,
                    )
                }

                is Effect.ArchiveStatusChanged -> {
                    snackbarScope.launchArchivedSnackbar(
                        snackbarHostState = snackbarHostState,
                        context = currentContext,
                        undoLabel = currentUndoLabel,
                        effect = effect,
                        onAction = currentOnAction,
                    )
                }

                is Effect.ConversationBlocked -> {
                    snackbarScope.launchBlockedSnackbar(
                        snackbarHostState = snackbarHostState,
                        context = currentContext,
                        undoLabel = currentUndoLabel,
                        effect = effect,
                        onAction = currentOnAction,
                    )
                }

                is Effect.PreparePinAnimation -> {
                    preparePinAnimation(
                        controller = pinAnimationController,
                        effect = effect,
                        onAction = currentOnAction,
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

private fun preparePinAnimation(
    controller: OverlayReorderAnimationController<ConversationListItemUiModel, String>,
    effect: Effect.PreparePinAnimation,
    onAction: (Action) -> Unit,
) {
    controller.prepare(
        keys = effect.conversationIds,
        anchorToTop = effect.isPinned,
        transform = { item ->
            item.copy(
                isPinned = effect.isPinned,
                isSelected = false,
            )
        },
    )

    onAction(
        Action.PinAnimationPrepared(
            conversationIds = effect.conversationIds,
            isPinned = effect.isPinned,
        ),
    )

    controller.markCommitted()
}

private fun CoroutineScope.launchArchivedSnackbar(
    snackbarHostState: SnackbarHostState,
    context: Context,
    undoLabel: String,
    effect: Effect.ArchiveStatusChanged,
    onAction: (Action) -> Unit,
) {
    val messageResId = when {
        effect.isArchived -> R.string.archived_toast_message
        else -> R.string.unarchived_toast_message
    }

    launch {
        val undoClicked = snackbarHostState.showActionSnackbar(
            message = context.getString(messageResId, effect.conversationIds.size),
            actionLabel = undoLabel,
        )

        if (undoClicked) {
            onAction(
                Action.ArchiveUndoClicked(
                    conversationIds = effect.conversationIds,
                    isArchived = effect.isArchived,
                ),
            )
        }
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
        val undoClicked = snackbarHostState.showActionSnackbar(
            message = context.getString(R.string.update_destination_blocked),
            actionLabel = undoLabel,
        )

        if (undoClicked) {
            onAction(
                Action.BlockUndoClicked(
                    conversationId = effect.conversationId,
                    destination = effect.destination,
                ),
            )
        }
    }
}

@Composable
private fun ConversationListScaffold(
    uiState: State,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    pinAnimationController: OverlayReorderAnimationController<ConversationListItemUiModel, String>?,
    onAction: (Action) -> Unit,
    onDeleteClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelectionMode = uiState.selection.selectedCount > 0
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
                onSnoozeClick = onSnoozeClick,
            )
        },
        snackbarHost = {
            MessagingSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = FabBottomReserve),
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
                .onGloballyPositioned { coordinates ->
                    pinAnimationController?.updateContentTop(coordinates.boundsInRoot().top)
                }
                .background(backdropColor)
                .clip(ContentCornerShape)
                .background(MaterialTheme.colorScheme.background),
        ) {
            ConversationListContent(
                content = uiState.content,
                listState = listState,
                onAction = onAction,
                contentPadding = contentPadding,
                isSelectionMode = isSelectionMode,
                bottomReserve = FabBottomReserve,
                pinAnimationController = pinAnimationController,
            )

            ConversationListFabs(
                uiState = uiState,
                isSelectionMode = isSelectionMode,
                onAction = onAction,
                onScrollToTop = onScrollToTop,
            )
        }
    }
}

@Composable
private fun BoxScope.ConversationListFabs(
    uiState: State,
    isSelectionMode: Boolean,
    onAction: (Action) -> Unit,
    onScrollToTop: () -> Unit,
) {
    ScrollToTopFab(
        visible = uiState.isScrollToTopVisible,
        onClick = onScrollToTop,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = FabSpacing),
    )

    StartChatFab(
        visible = !isSelectionMode,
        expanded = !uiState.isScrollToTopVisible,
        onClick = { onAction(Action.StartChatClicked) },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(FabSpacing),
    )
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
    onSnoozeClick: () -> Unit,
) {
    when {
        isSelectionMode -> {
            ConversationListSelectionTopAppBar(
                selectedCount = uiState.selection.selectedCount,
                actions = uiState.selection.actions,
                onAction = onAction,
                onDeleteClick = onDeleteClick,
                onSnoozeClick = onSnoozeClick,
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
        PrimaryActionButton(
            text = stringResource(R.string.conversation_list_start_chat),
            onClick = onClick,
            expanded = expanded,
            leadingIcon = Icons.AutoMirrored.Rounded.Chat,
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
            pinAnimationController = null,
            onAction = {},
            onDeleteClick = {},
            onSnoozeClick = {},
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
            pinAnimationController = null,
            onAction = {},
            onDeleteClick = {},
            onSnoozeClick = {},
            onScrollToTop = {},
        )
    }
}
