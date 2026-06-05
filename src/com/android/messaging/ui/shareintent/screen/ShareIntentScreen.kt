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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
import com.android.messaging.ui.shareintent.common.SelectedTargetsBar
import com.android.messaging.ui.shareintent.common.ShareAttachmentPreview
import com.android.messaging.ui.shareintent.common.ShareConfirmTopAppBar
import com.android.messaging.ui.shareintent.common.ShareIntentTopAppBar
import com.android.messaging.ui.shareintent.common.contentSurfaceShape
import com.android.messaging.ui.shareintent.common.shareComposeSubjectSlot
import com.android.messaging.ui.shareintent.screen.model.ShareDraftUiState
import com.android.messaging.ui.shareintent.screen.model.ShareIntentAction as Action
import com.android.messaging.ui.shareintent.screen.model.ShareIntentUiState as State
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import com.android.messaging.ui.shareintent.screen.model.ShareTargetsUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ShareIntentScreen(
    isInitialDraftLoading: Boolean,
    initialDraft: ConversationDraft?,
    effectHandler: ShareIntentEffectHandler,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    allowMultiSelect: Boolean = true,
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
        allowMultiSelect = allowMultiSelect,
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
    allowMultiSelect: Boolean = true,
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
            allowMultiSelect = allowMultiSelect,
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
    allowMultiSelect: Boolean,
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
                        allowMultiSelect = allowMultiSelect,
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
                        ShareTargetUiState.Contact(
                            contactId = 11L,
                            destination = "+31 6 1111 2222",
                            normalizedDestination = "+31611112222",
                            displayName = "Amelia Brown",
                            details = "+31 6 1111 2222",
                            avatarUri = null,
                        ),
                        ShareTargetUiState.Contact(
                            contactId = 12L,
                            destination = "+31 6 3333 4444",
                            normalizedDestination = "+31633334444",
                            displayName = "Brian Cohen",
                            details = "+31 6 3333 4444",
                            avatarUri = null,
                        ),
                        ShareTargetUiState.Contact(
                            contactId = 13L,
                            destination = "+1 555 0100",
                            normalizedDestination = "+15550100",
                            displayName = "+1 555 0100",
                            details = "+1 555 0100",
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

@PreviewLightDark
@Composable
private fun ShareIntentContactsPermissionPreview() {
    MessagingPreviewTheme {
        ShareIntentContent(
            uiState = State(
                targets = ShareTargetsUiState(
                    isLoading = false,
                    hasContactsPermission = false,
                ),
                draft = ShareDraftUiState(isLoading = false),
            ),
            onAction = {},
            onNavigateBack = {},
            onGrantContactsPermission = {},
        )
    }
}
