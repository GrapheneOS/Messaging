package com.android.messaging.ui.conversationpicker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.ui.common.components.composer.MESSAGE_COMPOSE_FIELD_TEST_TAG
import com.android.messaging.ui.common.components.composer.MessageComposeBar
import com.android.messaging.ui.common.components.composer.MessageSendButton
import com.android.messaging.ui.common.components.contentSurfaceShape
import com.android.messaging.ui.common.components.mediapreview.MediaPreviewBackground
import com.android.messaging.ui.common.components.selection.LocalSelectionListItemColors
import com.android.messaging.ui.common.components.selection.SelectionListContent
import com.android.messaging.ui.common.components.selection.selectionListItemColors
import com.android.messaging.ui.conversationpicker.common.PickerReviewAttachments
import com.android.messaging.ui.conversationpicker.common.PickerReviewTopAppBar
import com.android.messaging.ui.conversationpicker.common.PickerTopAppBar
import com.android.messaging.ui.conversationpicker.common.ScreenContentPadding
import com.android.messaging.ui.conversationpicker.common.SelectedTargetsBar
import com.android.messaging.ui.conversationpicker.common.composeSubjectSlot
import com.android.messaging.ui.conversationpicker.model.ConversationPickerAction as Action
import com.android.messaging.ui.conversationpicker.model.ConversationPickerLabels
import com.android.messaging.ui.conversationpicker.model.ConversationPickerUiState as State
import com.android.messaging.ui.conversationpicker.model.DraftUiState
import com.android.messaging.ui.conversationpicker.model.RecentTargetsUiState
import com.android.messaging.ui.conversationpicker.model.SelectionUiState
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import com.android.messaging.ui.conversationpicker.model.TargetsUiState
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.ui.recipientselection.component.RecipientSelectionContactsContent
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.subscription.component.SimSelectorRow
import com.android.messaging.ui.subscription.mapper.rememberSimSelectorUiState
import com.android.messaging.ui.subscription.model.SimSelectionUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ConversationPickerScreen(
    isInitialDraftLoading: Boolean,
    initialDraft: ConversationDraft?,
    effectHandler: ConversationPickerEffectHandler,
    onNavigateBack: () -> Unit,
    allowMultiSelect: Boolean,
    labels: ConversationPickerLabels,
    modifier: Modifier = Modifier,
    screenModel: ConversationPickerScreenModel = viewModel<ConversationPickerViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    val currentEffectHandler by rememberUpdatedState(effectHandler)
    LaunchedEffect(screenModel) {
        screenModel.effects.collect { effect ->
            currentEffectHandler.handle(effect)
        }
    }

    LaunchedEffect(isInitialDraftLoading) {
        if (!isInitialDraftLoading) {
            screenModel.onAction(Action.DraftResolved(initialDraft))
        }
    }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            screenModel.onAction(Action.ContactsPermissionGranted)
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        if (isReadContactsPermissionGranted(context)) {
            screenModel.onAction(Action.ContactsPermissionGranted)
        }
    }

    PickerContent(
        uiState = uiState,
        onAction = screenModel::onAction,
        onNavigateBack = onNavigateBack,
        onGrantContactsPermission = {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        },
        allowMultiSelect = allowMultiSelect,
        labels = labels,
        modifier = modifier,
    )
}

private fun isReadContactsPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS,
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun PickerContent(
    uiState: State,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    onGrantContactsPermission: () -> Unit,
    allowMultiSelect: Boolean,
    labels: ConversationPickerLabels,
    modifier: Modifier = Modifier,
) {
    val searchState = rememberTextFieldState()

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .collect { query ->
                onAction(Action.SearchQueryChanged(query))
            }
    }

    PickerBackHandlers(
        uiState = uiState,
        searchState = searchState,
        onAction = onAction,
    )

    if (uiState.draft.isReviewing) {
        PickerReviewScaffold(
            uiState = uiState,
            onAction = onAction,
            labels = labels,
            modifier = modifier,
        )
    } else {
        PickerScaffold(
            uiState = uiState,
            searchState = searchState,
            onAction = onAction,
            onNavigateBack = onNavigateBack,
            onGrantContactsPermission = onGrantContactsPermission,
            allowMultiSelect = allowMultiSelect,
            labels = labels,
            modifier = modifier,
        )
    }
}

@Composable
private fun PickerBackHandlers(
    uiState: State,
    searchState: TextFieldState,
    onAction: (Action) -> Unit,
) {
    val isReviewing = uiState.draft.isReviewing
    val inSelectionMode = uiState.targets.selection.selectedIds.isNotEmpty()
    val isSearchActive = uiState.targets.isSearchActive

    BackHandler(enabled = isReviewing || inSelectionMode || isSearchActive) {
        when {
            isReviewing -> onAction(Action.ReviewDismissed)
            isSearchActive -> {
                searchState.clearText()
                onAction(Action.SearchClosed)
            }
            inSelectionMode -> onAction(Action.SelectionCleared)
        }
    }
}

@Composable
private fun PickerScaffold(
    uiState: State,
    searchState: TextFieldState,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    onGrantContactsPermission: () -> Unit,
    allowMultiSelect: Boolean,
    labels: ConversationPickerLabels,
    modifier: Modifier = Modifier,
) {
    val inSelectionMode = uiState.targets.selection.selectedIds.isNotEmpty()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            PickerTopBar(
                uiState = uiState,
                searchState = searchState,
                inSelectionMode = inSelectionMode,
                onAction = onAction,
                onNavigateBack = onNavigateBack,
                labels = labels,
            )
        },
    ) { contentPadding ->
        CompositionLocalProvider(
            LocalSelectionListItemColors provides selectionListItemColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = contentPadding.calculateTopPadding())
                    .clip(MaterialTheme.contentSurfaceShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    else -> {
                        PickerTargetsContent(
                            uiState = uiState,
                            inSelectionMode = inSelectionMode,
                            allowMultiSelect = allowMultiSelect,
                            onAction = onAction,
                            onGrantContactsPermission = onGrantContactsPermission,
                            bottomPadding = contentPadding.calculateBottomPadding(),
                            labels = labels,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerTargetsContent(
    uiState: State,
    inSelectionMode: Boolean,
    allowMultiSelect: Boolean,
    onAction: (Action) -> Unit,
    onGrantContactsPermission: () -> Unit,
    bottomPadding: Dp,
    labels: ConversationPickerLabels,
    modifier: Modifier = Modifier,
) {
    if (!uiState.contacts.hasContactsPermission) {
        PickerRecentTargetsContent(
            uiState = uiState,
            inSelectionMode = inSelectionMode,
            allowMultiSelect = allowMultiSelect,
            onAction = onAction,
            onGrantContactsPermission = onGrantContactsPermission,
            bottomPadding = bottomPadding,
            labels = labels,
            modifier = modifier,
        )
        return
    }

    PickerContactsTargetsContent(
        uiState = uiState,
        inSelectionMode = inSelectionMode,
        allowMultiSelect = allowMultiSelect,
        onAction = onAction,
        onGrantContactsPermission = onGrantContactsPermission,
        bottomPadding = bottomPadding,
        labels = labels,
        modifier = modifier,
    )
}

@Composable
private fun PickerRecentTargetsContent(
    uiState: State,
    inSelectionMode: Boolean,
    allowMultiSelect: Boolean,
    onAction: (Action) -> Unit,
    onGrantContactsPermission: () -> Unit,
    bottomPadding: Dp,
    labels: ConversationPickerLabels,
    modifier: Modifier = Modifier,
) {
    SelectionListContent(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ScreenContentPadding,
            top = ScreenContentPadding,
            end = ScreenContentPadding,
            bottom = bottomPadding,
        ),
        canLoadMore = false,
        isLoading = false,
        isLoadingMore = false,
        loadMoreItemCount = 0,
        onLoadMore = {},
    ) {
        item(key = "recent_targets") {
            RecentTargetsSection(
                recentTargets = uiState.targets.recent.targets,
                selectedIds = uiState.targets.selection.selectedIds,
                inSelectionMode = inSelectionMode,
                allowMultiSelect = allowMultiSelect,
                canLoadMoreRecent = uiState.targets.recent.canLoadMore,
                canCollapseRecent = uiState.targets.recent.canCollapse,
                hasContactsPermission = uiState.contacts.hasContactsPermission,
                onAction = onAction,
                onGrantContactsPermission = onGrantContactsPermission,
                recentConversationsTitle = labels.recentConversationsTitle,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun PickerContactsTargetsContent(
    uiState: State,
    inSelectionMode: Boolean,
    allowMultiSelect: Boolean,
    onAction: (Action) -> Unit,
    onGrantContactsPermission: () -> Unit,
    bottomPadding: Dp,
    labels: ConversationPickerLabels,
    modifier: Modifier = Modifier,
) {
    RecipientSelectionContactsContent(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ScreenContentPadding,
            top = ScreenContentPadding,
            end = ScreenContentPadding,
            bottom = bottomPadding,
        ),
        uiState = uiState.asRecipientSelectionState(),
        emptyStateText = labels.emptyStateText,
        rowDecorators = pickerContactRowDecorators,
        onLoadMore = { onAction(Action.LoadMoreContacts) },
        onPrimaryActionClick = {},
        onRecipientDestinationClick = { item, destination ->
            onAction(
                contactDestinationAction(
                    item = item,
                    destination = destination,
                    inSelectionMode = inSelectionMode,
                ),
            )
        },
        onRecipientDestinationLongClick = when {
            allowMultiSelect -> {
                { item, destination ->
                    onAction(Action.ContactDestinationToggled(item, destination))
                }
            }

            else -> null
        },
        topListContent = when {
            uiState.targets.isSearchActive -> null
            else -> {
                {
                    RecentTargetsSection(
                        recentTargets = uiState.targets.recent.targets,
                        selectedIds = uiState.targets.selection.selectedIds,
                        inSelectionMode = inSelectionMode,
                        allowMultiSelect = allowMultiSelect,
                        canLoadMoreRecent = uiState.targets.recent.canLoadMore,
                        canCollapseRecent = uiState.targets.recent.canCollapse,
                        hasContactsPermission = uiState.contacts.hasContactsPermission,
                        onAction = onAction,
                        onGrantContactsPermission = onGrantContactsPermission,
                        recentConversationsTitle = labels.recentConversationsTitle,
                    )
                }
            }
        },
    )
}

@Composable
private fun PickerTopBar(
    uiState: State,
    searchState: TextFieldState,
    inSelectionMode: Boolean,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    labels: ConversationPickerLabels,
) {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        PickerTopAppBar(
            isSearchActive = uiState.targets.isSearchActive,
            inSelectionMode = inSelectionMode,
            selectedCount = uiState.targets.selection.selectedIds.size,
            searchState = searchState,
            title = labels.title,
            searchHint = labels.searchHint,
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
                targets = uiState.targets.selection.selectedTargets,
                onRemove = { onAction(Action.SelectionToggled(it)) },
                onProceed = { onAction(Action.ProceedToReviewClicked) },
                showProceedButton = true,
            )
        }
    }
}

@Composable
private fun PickerReviewScaffold(
    uiState: State,
    onAction: (Action) -> Unit,
    labels: ConversationPickerLabels,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                PickerReviewTopAppBar(
                    title = labels.title,
                    onBack = { onAction(Action.ReviewDismissed) },
                )

                SelectedTargetsBar(
                    targets = uiState.targets.selection.selectedTargets,
                    onRemove = { onAction(Action.SelectionToggled(it)) },
                    onProceed = {},
                    showProceedButton = false,
                )
            }
        },
    ) { contentPadding ->
        PickerReviewContent(
            uiState = uiState,
            onAction = onAction,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
                .clip(MaterialTheme.contentSurfaceShape)
                .background(MaterialTheme.colorScheme.background),
        )
    }
}

@Composable
private fun PickerReviewContent(
    uiState: State,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val attachments = uiState.draft.attachments
    val pagerState = rememberPagerState(pageCount = { attachments.size })

    Box(
        modifier = modifier,
    ) {
        if (attachments.isNotEmpty()) {
            MediaPreviewBackground(
                modifier = Modifier.matchParentSize(),
                items = uiState.draft.backgroundItems,
                pagerState = pagerState,
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .weight(weight = 1f)
                    .fillMaxWidth(),
            ) {
                PickerReviewAttachments(
                    attachments = attachments,
                    pagerState = pagerState,
                    onAttachmentClick = { onAction(Action.DraftAttachmentClicked(it)) },
                    onAttachmentRemove = { onAction(Action.DraftAttachmentRemoved(it)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            PickerReviewSimSelector(
                sim = uiState.sim,
                onSimSelected = { onAction(Action.SimSelected(it)) },
            )

            PickerReviewComposeBar(
                uiState = uiState,
                onAction = onAction,
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.ime.union(WindowInsets.navigationBars),
                ),
            )
        }
    }
}

@Composable
private fun PickerReviewSimSelector(
    sim: SimSelectionUiState,
    onSimSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val simSelectorUiState = rememberSimSelectorUiState(
        subscriptions = sim.subscriptions,
        selectedSelfParticipantId = sim.selectedSelfParticipantId,
    )

    if (!simSelectorUiState.isAvailable) {
        return
    }

    val selectedLabel = simSelectorUiState.selectedOption?.label.orEmpty()

    SimSelectorRow(
        modifier = modifier,
        uiState = simSelectorUiState,
        prefixText = stringResource(id = R.string.new_chat_sim_selector_prefix),
        chipContentDescription = stringResource(
            id = R.string.new_chat_sim_selector_chip_content_description,
            selectedLabel,
        ),
        selectedContentDescription = stringResource(id = R.string.sim_selector_item_selected),
        onSimSelected = onSimSelected,
    )
}

@Composable
private fun PickerReviewComposeBar(
    uiState: State,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageComposeBar(
        modifier = modifier,
        text = uiState.draft.text,
        onTextChange = { onAction(Action.DraftTextChanged(it)) },
        isFieldEnabled = true,
        isFieldContentHidden = false,
        fieldFocusRequester = null,
        fieldStateDescription = null,
        fieldTestTag = MESSAGE_COMPOSE_FIELD_TEST_TAG,
        topContent = composeSubjectSlot(
            subjectText = uiState.draft.subjectText,
            onClear = { onAction(Action.DraftSubjectCleared) },
        ),
        sendAction = {
            MessageSendButton(
                enabled = uiState.isSendEnabled,
                onClick = { onAction(Action.SendClicked) },
            )
        },
    )
}

@PreviewLightDark
@Composable
private fun PickerContentPreview() {
    val targets = persistentListOf(
        TargetUiState.Conversation(
            conversationId = "1",
            normalizedDestination = "+31612345678",
            displayName = "Jane Doe",
            details = "+31 6 1234 5678",
            avatarUri = null,
            isGroup = false,
        ),
        TargetUiState.Conversation(
            conversationId = "2",
            normalizedDestination = null,
            displayName = "Project group",
            details = null,
            avatarUri = null,
            isGroup = true,
        ),
    )

    MessagingPreviewTheme {
        PickerContent(
            uiState = State(
                targets = TargetsUiState(
                    isLoading = false,
                    recent = RecentTargetsUiState(targets = targets),
                ),
                draft = DraftUiState(isLoading = false),
            ),
            onAction = {},
            onNavigateBack = {},
            onGrantContactsPermission = {},
            allowMultiSelect = true,
            labels = ConversationPickerLabels.Share,
        )
    }
}

@PreviewLightDark
@Composable
private fun PickerSelectionPreview() {
    val targets = persistentListOf(
        TargetUiState.Conversation(
            conversationId = "1",
            normalizedDestination = "+31612345678",
            displayName = "Jane Doe",
            details = "+31 6 1234 5678",
            avatarUri = null,
            isGroup = false,
        ),
        TargetUiState.Conversation(
            conversationId = "2",
            normalizedDestination = null,
            displayName = "Project group",
            details = null,
            avatarUri = null,
            isGroup = true,
        ),
    )

    MessagingPreviewTheme {
        PickerContent(
            uiState = State(
                targets = TargetsUiState(
                    isLoading = false,
                    recent = RecentTargetsUiState(targets = targets),
                    selection = SelectionUiState(
                        selectedIds = persistentSetOf(
                            "dest:+31612345678",
                            "conversation:2",
                        ),
                        selectedTargets = targets,
                    ),
                ),
                draft = DraftUiState(isLoading = false),
            ),
            onAction = {},
            onNavigateBack = {},
            onGrantContactsPermission = {},
            allowMultiSelect = true,
            labels = ConversationPickerLabels.Share,
        )
    }
}

@PreviewLightDark
@Composable
private fun PickerEmptyPreview() {
    MessagingPreviewTheme {
        PickerContent(
            uiState = State(
                targets = TargetsUiState(isLoading = false),
                draft = DraftUiState(isLoading = false),
            ),
            onAction = {},
            onNavigateBack = {},
            onGrantContactsPermission = {},
            allowMultiSelect = true,
            labels = ConversationPickerLabels.Share,
        )
    }
}

@PreviewLightDark
@Composable
private fun PickerContactsPermissionPreview() {
    MessagingPreviewTheme {
        PickerContent(
            uiState = State(
                targets = TargetsUiState(isLoading = false),
                contacts = RecipientPickerUiState(hasContactsPermission = false),
                draft = DraftUiState(isLoading = false),
            ),
            onAction = {},
            onNavigateBack = {},
            onGrantContactsPermission = {},
            allowMultiSelect = true,
            labels = ConversationPickerLabels.Share,
        )
    }
}
