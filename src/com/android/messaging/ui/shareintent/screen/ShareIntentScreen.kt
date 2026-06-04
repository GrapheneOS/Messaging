package com.android.messaging.ui.shareintent.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.ui.common.components.composer.MESSAGE_COMPOSE_FIELD_TEST_TAG
import com.android.messaging.ui.common.components.composer.MessageComposeBar
import com.android.messaging.ui.common.components.composer.MessageSendButton
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.ui.shareintent.common.ItemDividerHorizontalInset
import com.android.messaging.ui.shareintent.common.NewMessageItem
import com.android.messaging.ui.shareintent.common.ScreenContentPadding
import com.android.messaging.ui.shareintent.common.SelectedTargetsBar
import com.android.messaging.ui.shareintent.common.ShareAttachmentPreview
import com.android.messaging.ui.shareintent.common.ShareConfirmTopAppBar
import com.android.messaging.ui.shareintent.common.ShareIntentTopAppBar
import com.android.messaging.ui.shareintent.common.ShareTargetItem
import com.android.messaging.ui.shareintent.common.contentSurfaceShape
import com.android.messaging.ui.shareintent.common.shareComposeSubjectSlot
import com.android.messaging.ui.shareintent.screen.model.ShareDraftUiState
import com.android.messaging.ui.shareintent.screen.model.ShareIntentAction as Action
import com.android.messaging.ui.shareintent.screen.model.ShareIntentUiState as State
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import com.android.messaging.ui.shareintent.screen.model.ShareTargetsUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

private val ContactsSectionHeaderVerticalPadding = 8.dp
private val ContactsPermissionActionSpacing = 4.dp
private const val LOAD_MORE_PREFETCH_DISTANCE = 10

@Composable
internal fun ShareIntentScreen(
    isInitialDraftLoading: Boolean,
    initialDraft: ConversationDraft?,
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

    LifecycleResumeEffect(context) {
        if (isReadContactsPermissionGranted(context)) {
            screenModel.onAction(Action.ContactsPermissionGranted)
        }
        onPauseOrDispose {}
    }

    ShareIntentContent(
        uiState = uiState,
        onAction = screenModel::onAction,
        onNavigateBack = onNavigateBack,
        onGrantContactsPermission = {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        },
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
private fun ShareIntentContent(
    uiState: State,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    onGrantContactsPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchState = rememberTextFieldState()

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .collect { query ->
                onAction(Action.SearchQueryChanged(query))
            }
    }

    ShareIntentBackHandlers(
        uiState = uiState,
        searchState = searchState,
        onAction = onAction,
    )

    if (uiState.draft.isReviewing) {
        ShareConfirmScreen(
            uiState = uiState,
            onAction = onAction,
            modifier = modifier,
        )
    } else {
        ShareIntentPickerScaffold(
            uiState = uiState,
            searchState = searchState,
            onAction = onAction,
            onNavigateBack = onNavigateBack,
            onGrantContactsPermission = onGrantContactsPermission,
            modifier = modifier,
        )
    }
}

@Composable
private fun ShareIntentBackHandlers(
    uiState: State,
    searchState: TextFieldState,
    onAction: (Action) -> Unit,
) {
    BackHandler(enabled = uiState.draft.isReviewing) {
        onAction(Action.ReviewDismissed)
    }

    BackHandler(enabled = !uiState.draft.isReviewing && uiState.targets.selectedIds.isNotEmpty()) {
        onAction(Action.SelectionCleared)
    }

    BackHandler(enabled = !uiState.draft.isReviewing && uiState.targets.isSearchActive) {
        searchState.clearText()
        onAction(Action.SearchClosed)
    }
}

@Composable
private fun ShareIntentPickerScaffold(
    uiState: State,
    searchState: TextFieldState,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    onGrantContactsPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inSelectionMode = uiState.targets.selectedIds.isNotEmpty()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            ShareIntentPickerTopBar(
                uiState = uiState,
                searchState = searchState,
                inSelectionMode = inSelectionMode,
                onAction = onAction,
                onNavigateBack = onNavigateBack,
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
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    ShareTargetList(
                        recentTargets = uiState.targets.recentTargets,
                        contactTargets = uiState.targets.contactTargets,
                        selectedIds = uiState.targets.selectedIds,
                        inSelectionMode = inSelectionMode,
                        showNewMessage = !uiState.targets.isSearchActive && !inSelectionMode,
                        canLoadMoreRecent = uiState.targets.canLoadMoreRecent,
                        canCollapseRecent = uiState.targets.canCollapseRecent,
                        hasContactsPermission = uiState.targets.hasContactsPermission,
                        canLoadMoreContacts = uiState.targets.canLoadMoreContacts,
                        onAction = onAction,
                        onGrantContactsPermission = onGrantContactsPermission,
                        bottomPadding = contentPadding.calculateBottomPadding(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareIntentPickerTopBar(
    uiState: State,
    searchState: TextFieldState,
    inSelectionMode: Boolean,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        ShareIntentTopAppBar(
            isSearchActive = uiState.targets.isSearchActive,
            inSelectionMode = inSelectionMode,
            selectedCount = uiState.targets.selectedIds.size,
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
                targets = uiState.targets.selectedTargets,
                onRemove = { onAction(Action.SelectionToggled(it)) },
                onSend = { onAction(Action.SendToSelectedClicked) },
                showSendButton = true,
            )
        }
    }
}

@Composable
private fun ShareConfirmScreen(
    uiState: State,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                ShareConfirmTopAppBar(
                    onBack = { onAction(Action.ReviewDismissed) },
                )

                SelectedTargetsBar(
                    targets = uiState.targets.selectedTargets,
                    onRemove = { onAction(Action.SelectionToggled(it)) },
                    onSend = {},
                    showSendButton = false,
                )
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
            MessageComposeBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.ime.union(WindowInsets.navigationBars),
                    ),
                text = uiState.draft.text,
                onTextChange = { onAction(Action.DraftTextChanged(it)) },
                isFieldEnabled = true,
                isFieldContentHidden = false,
                fieldFocusRequester = null,
                fieldStateDescription = null,
                fieldTestTag = MESSAGE_COMPOSE_FIELD_TEST_TAG,
                topContent = shareComposeSubjectSlot(
                    subjectText = uiState.draft.subjectText,
                    onClear = { onAction(Action.DraftSubjectCleared) },
                ),
                sendAction = {
                    MessageSendButton(
                        enabled = uiState.isSendEnabled,
                        onClick = { onAction(Action.ConfirmSendClicked) },
                    )
                },
                attachmentsContent = {
                    ShareAttachmentPreview(
                        attachments = uiState.draft.attachments,
                        onRemove = { onAction(Action.DraftAttachmentRemoved(it)) },
                    )
                },
            )
        }
    }
}

@Composable
private fun ShareTargetList(
    recentTargets: List<ShareTargetUiState>,
    contactTargets: List<ShareTargetUiState>,
    selectedIds: Set<String>,
    inSelectionMode: Boolean,
    showNewMessage: Boolean,
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
            showNewMessage = showNewMessage,
            canLoadMoreRecent = canLoadMoreRecent,
            canCollapseRecent = canCollapseRecent,
            onAction = onAction,
        )

        contactsSection(
            contactTargets = contactTargets,
            selectedIds = selectedIds,
            inSelectionMode = inSelectionMode,
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
    showNewMessage: Boolean,
    canLoadMoreRecent: Boolean,
    canCollapseRecent: Boolean,
    onAction: (Action) -> Unit,
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
        items = recentTargets,
        key = { _, target -> target.key },
    ) { index, target ->
        Column(modifier = Modifier.animateItem()) {
            if (showNewMessage || index > 0) {
                ItemDivider()
            }

            ShareTargetRow(
                target = target,
                selectedIds = selectedIds,
                inSelectionMode = inSelectionMode,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ItemDividerHorizontalInset),
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
            item(key = "contacts_header") {
                ContactsSectionHeader(modifier = Modifier.animateItem())
            }

            itemsIndexed(
                items = contactTargets,
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
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareTargetRow(
    target: ShareTargetUiState,
    selectedIds: Set<String>,
    inSelectionMode: Boolean,
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
        },
    )
}

@Composable
private fun ContactsSectionHeader(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.share_contacts_section_title),
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ItemDividerHorizontalInset,
                vertical = ContactsSectionHeaderVerticalPadding,
            ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ContactsPermissionPrompt(
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ItemDividerHorizontalInset,
                vertical = ContactsSectionHeaderVerticalPadding,
            ),
    ) {
        Text(
            text = stringResource(R.string.share_contacts_permission_rationale),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TextButton(
            onClick = onGrant,
            modifier = Modifier.padding(top = ContactsPermissionActionSpacing),
        ) {
            Text(text = stringResource(R.string.share_contacts_permission_action))
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
            vertical = 1.dp,
        ),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

@PreviewLightDark
@Composable
private fun ShareIntentContentPreview() {
    MessagingPreviewTheme {
        ShareIntentContent(
            uiState = State(
                targets = ShareTargetsUiState(
                    isLoading = false,
                    recentTargets = persistentListOf(
                        ShareTargetUiState.Conversation(
                            conversationId = "1",
                            normalizedDestination = "+31612345678",
                            displayName = "Jane Doe",
                            details = "+31 6 1234 5678",
                            avatarUri = null,
                            isGroup = false,
                        ),
                        ShareTargetUiState.Conversation(
                            conversationId = "2",
                            normalizedDestination = null,
                            displayName = "Project group",
                            details = null,
                            avatarUri = null,
                            isGroup = true,
                        ),
                    ),
                    contactTargets = persistentListOf(
                        ShareTargetUiState.Contact(
                            contactId = 10L,
                            destination = "+31 6 9999 0000",
                            normalizedDestination = "+31699990000",
                            displayName = "Alex Appleseed",
                            details = "+31 6 9999 0000",
                            avatarUri = null,
                        ),
                    ),
                ),
                draft = ShareDraftUiState(isLoading = false),
            ),
            onAction = {},
            onNavigateBack = {},
            onGrantContactsPermission = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ShareIntentSelectionPreview() {
    val targets = persistentListOf(
        ShareTargetUiState.Conversation(
            conversationId = "1",
            normalizedDestination = "+31612345678",
            displayName = "Jane Doe",
            details = "+31 6 1234 5678",
            avatarUri = null,
            isGroup = false,
        ),
        ShareTargetUiState.Conversation(
            conversationId = "2",
            normalizedDestination = null,
            displayName = "Project group",
            details = null,
            avatarUri = null,
            isGroup = true,
        ),
    )

    MessagingPreviewTheme {
        ShareIntentContent(
            uiState = State(
                targets = ShareTargetsUiState(
                    isLoading = false,
                    recentTargets = targets,
                    selectedIds = persistentSetOf("dest:+31612345678", "conversation:2"),
                    selectedTargets = targets,
                ),
                draft = ShareDraftUiState(isLoading = false),
            ),
            onAction = {},
            onNavigateBack = {},
            onGrantContactsPermission = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ShareIntentEmptyPreview() {
    MessagingPreviewTheme {
        ShareIntentContent(
            uiState = State(
                targets = ShareTargetsUiState(isLoading = false),
                draft = ShareDraftUiState(isLoading = false),
            ),
            onAction = {},
            onNavigateBack = {},
            onGrantContactsPermission = {},
        )
    }
}
