package com.android.messaging.ui.conversationlist.archived

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.ui.common.components.snackbar.MessagingSnackbarHost
import com.android.messaging.ui.common.components.snackbar.showActionSnackbar
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListAction as Action
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListEffect as Effect
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListUiState as State
import com.android.messaging.ui.conversationlist.common.dialog.ConversationListDeleteDialog
import com.android.messaging.ui.conversationlist.common.item.ConversationSwipeKind
import com.android.messaging.ui.conversationlist.common.list.ConversationListItemEvent
import com.android.messaging.ui.conversationlist.common.list.ConversationListItems
import com.android.messaging.ui.conversationlist.common.list.ConversationListSwipeSpec
import com.android.messaging.ui.conversationlist.common.list.unsupportedSwipeKind
import com.android.messaging.ui.conversationlist.common.status.ConversationListLoadingIndicator
import com.android.messaging.ui.conversationlist.common.status.ConversationListStatusMessage
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItems
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private val ContentCornerShape = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 28.dp,
)

private val ArchivedSwipeSpec = ConversationListSwipeSpec(
    startToEnd = ConversationSwipeKind.Unarchive,
    endToStart = ConversationSwipeKind.Unarchive,
)

@Composable
internal fun ArchivedConversationListScreen(
    effectHandler: ArchivedConversationListEffectHandler,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: ArchivedConversationListScreenModel =
        viewModel<ArchivedConversationListViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingDelete by remember { mutableStateOf(false) }

    ArchivedConversationListEffects(
        effects = screenModel.effects,
        effectHandler = effectHandler,
        snackbarHostState = snackbarHostState,
        onAction = screenModel::onAction,
    )

    ArchivedConversationListScaffold(
        uiState = uiState,
        listState = listState,
        snackbarHostState = snackbarHostState,
        onAction = screenModel::onAction,
        onNavigateBack = onNavigateBack,
        onDeleteClick = { pendingDelete = true },
        modifier = modifier,
    )

    if (pendingDelete) {
        ConversationListDeleteDialog(
            selectedCount = uiState.selectedCount,
            onConfirm = {
                screenModel.onAction(Action.DeleteSelectedConfirmed)
                pendingDelete = false
            },
            onDismiss = { pendingDelete = false },
        )
    }
}

@Composable
private fun ArchivedConversationListEffects(
    effects: Flow<Effect>,
    effectHandler: ArchivedConversationListEffectHandler,
    snackbarHostState: SnackbarHostState,
    onAction: (Action) -> Unit,
) {
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.snack_bar_undo)
    val snackbarScope = rememberCoroutineScope()

    val currentContext by rememberUpdatedState(context)
    val currentEffectHandler by rememberUpdatedState(effectHandler)
    val currentUndoLabel by rememberUpdatedState(undoLabel)
    val currentOnAction by rememberUpdatedState(onAction)

    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is Effect.ConversationsUnarchived -> {
                    snackbarScope.launchUnarchivedSnackbar(
                        snackbarHostState = snackbarHostState,
                        context = currentContext,
                        undoLabel = currentUndoLabel,
                        effect = effect,
                        onAction = currentOnAction,
                    )
                }

                else -> currentEffectHandler.handle(effect)
            }
        }
    }
}

private fun CoroutineScope.launchUnarchivedSnackbar(
    snackbarHostState: SnackbarHostState,
    context: Context,
    undoLabel: String,
    effect: Effect.ConversationsUnarchived,
    onAction: (Action) -> Unit,
) {
    launch {
        val undoClicked = snackbarHostState.showActionSnackbar(
            message = context.getString(
                R.string.unarchived_toast_message,
                effect.conversationIds.size,
            ),
            actionLabel = undoLabel,
        )

        if (undoClicked) {
            onAction(Action.UnarchiveUndoClicked(effect.conversationIds))
        } else {
            onAction(Action.UnarchiveSnackbarDismissed(effect.conversationIds))
        }
    }
}

@Composable
private fun ArchivedConversationListScaffold(
    uiState: State,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelectionMode = uiState.selectedCount > 0

    BackHandler(enabled = isSelectionMode) {
        onAction(Action.SelectionCleared)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            ArchivedConversationListTopBar(
                uiState = uiState,
                isSelectionMode = isSelectionMode,
                onAction = onAction,
                onNavigateBack = onNavigateBack,
                onDeleteClick = onDeleteClick,
            )
        },
        snackbarHost = {
            MessagingSnackbarHost(hostState = snackbarHostState)
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
                .background(archivedBackdropColor(isSelectionMode))
                .clip(ContentCornerShape)
                .background(MaterialTheme.colorScheme.background),
        ) {
            ArchivedConversationListContent(
                content = uiState.content,
                listState = listState,
                isSelectionMode = isSelectionMode,
                scaffoldContentPadding = contentPadding,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun ArchivedConversationListTopBar(
    uiState: State,
    isSelectionMode: Boolean,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    when {
        isSelectionMode -> ArchivedConversationListSelectionTopAppBar(
            selectedCount = uiState.selectedCount,
            onAction = onAction,
            onDeleteClick = onDeleteClick,
        )

        else -> ArchivedConversationListTopAppBar(
            isDebugEnabled = uiState.isDebugEnabled,
            onNavigateBack = onNavigateBack,
            onAction = onAction,
        )
    }
}

@Composable
private fun ArchivedConversationListContent(
    content: ConversationListContentUiState,
    listState: LazyListState,
    isSelectionMode: Boolean,
    scaffoldContentPadding: PaddingValues,
    onAction: (Action) -> Unit,
) {
    when (content) {
        ConversationListContentUiState.Loading -> {
            ConversationListLoadingIndicator()
        }

        ConversationListContentUiState.Empty -> {
            ConversationListStatusMessage(
                text = stringResource(R.string.archived_conversation_list_empty_text),
                modifier = Modifier
                    .padding(bottom = scaffoldContentPadding.calculateBottomPadding()),
            )
        }

        is ConversationListContentUiState.Items -> {
            ConversationListItems(
                items = content.items,
                restoredConversationIds = content.restoredConversationIds,
                listState = listState,
                isSelectionMode = isSelectionMode,
                scaffoldContentPadding = scaffoldContentPadding,
                fabBottomReserve = 0.dp,
                pinAnimationController = null,
                swipeSpec = ArchivedSwipeSpec,
                onItemEvent = { onAction(it.toArchivedAction()) },
            )
        }

        ConversationListContentUiState.WaitingForSync -> Unit
    }
}

@Composable
private fun archivedBackdropColor(isSelectionMode: Boolean): Color {
    return when {
        isSelectionMode -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
}

private fun ConversationListItemEvent.toArchivedAction(): Action {
    return when (this) {
        is ConversationListItemEvent.Clicked -> {
            Action.ConversationClicked(conversationId)
        }

        is ConversationListItemEvent.LongClicked -> {
            Action.ConversationLongClicked(conversationId)
        }

        is ConversationListItemEvent.AvatarMessageClicked -> {
            Action.AvatarMessageClicked(conversationId)
        }

        is ConversationListItemEvent.AvatarCallClicked -> {
            Action.AvatarCallClicked(destination)
        }

        is ConversationListItemEvent.AvatarContactClicked -> {
            Action.AvatarContactClicked(item.avatar)
        }

        is ConversationListItemEvent.AvatarInfoClicked -> {
            Action.AvatarInfoClicked(conversationId)
        }

        is ConversationListItemEvent.Swiped -> when (kind) {
            ConversationSwipeKind.Unarchive -> {
                Action.ConversationSwipedToUnarchive(conversationId)
            }

            ConversationSwipeKind.Archive -> unsupportedSwipeKind(kind)
            ConversationSwipeKind.ToggleRead -> unsupportedSwipeKind(kind)
        }
    }
}

@PreviewLightDark
@Composable
private fun ArchivedConversationListScaffoldItemsPreview() {
    MessagingPreviewTheme {
        ArchivedConversationListScaffold(
            uiState = State(
                content = ConversationListContentUiState.Items(
                    items = previewConversationListItems(),
                ),
            ),
            listState = rememberLazyListState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onNavigateBack = {},
            onDeleteClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ArchivedConversationListScaffoldEmptyPreview() {
    MessagingPreviewTheme {
        ArchivedConversationListScaffold(
            uiState = State(content = ConversationListContentUiState.Empty),
            listState = rememberLazyListState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onNavigateBack = {},
            onDeleteClick = {},
        )
    }
}
