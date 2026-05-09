package com.android.messaging.ui.conversation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.ui.conversation.CONVERSATION_LOADING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.composer.ui.ConversationComposerSection
import com.android.messaging.ui.conversation.composer.ui.ConversationSimSelectorSheet
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.mediapicker.rememberConversationMediaPickerPermissionState
import com.android.messaging.ui.conversation.mediapicker.rememberConversationMediaPickerState
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.messages.ui.ConversationMessages
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.metadata.ui.ConversationTopAppBar
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import kotlinx.collections.immutable.ImmutableList

private const val SMOOTH_SCROLL_JUMP_THRESHOLD = 15

@Composable
internal fun ConversationScreen(
    modifier: Modifier = Modifier,
    conversationId: String? = null,
    launchGeneration: Int? = null,
    cancelIncomingNotification: Boolean = true,
    onAddPeopleClick: () -> Unit,
    onConversationDetailsClick: () -> Unit,
    onNavigateBack: () -> Unit,
    pendingDraft: ConversationDraft? = null,
    pendingScrollPosition: Int? = null,
    pendingStartupAttachment: ConversationEntryStartupAttachment? = null,
    onPendingDraftConsumed: () -> Unit = {},
    onPendingScrollPositionConsumed: () -> Unit = {},
    onPendingStartupAttachmentConsumed: () -> Unit = {},
    screenModel: ConversationScreenModel = hiltViewModel<ConversationViewModel>(),
) {
    val messageFieldFocusRequester = remember { FocusRequester() }
    val mediaPickerState = rememberConversationMediaPickerState()
    val scaffoldUiState by screenModel.scaffoldUiState.collectAsStateWithLifecycle()
    val mediaPickerOverlayUiState by screenModel
        .mediaPickerOverlayUiState
        .collectAsStateWithLifecycle()

    val permissionState = rememberConversationMediaPickerPermissionState()

    val hostBoundsState = remember { mutableStateOf<ComposeRect?>(value = null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val onOpenContactPicker = rememberOpenContactPickerCallback(screenModel = screenModel)
    val requestAudioRecordingStart = rememberAudioRecordingStartRequest(
        screenModel = screenModel,
        permissionState = permissionState,
    )

    ConversationScreenRouteEffects(
        conversationId = conversationId,
        launchGeneration = launchGeneration,
        cancelIncomingNotification = cancelIncomingNotification,
        pendingDraft = pendingDraft,
        pendingStartupAttachment = pendingStartupAttachment,
        scaffoldUiState = scaffoldUiState,
        snackbarHostState = snackbarHostState,
        hostBoundsState = hostBoundsState,
        permissionState = permissionState,
        screenModel = screenModel,
        onNavigateBack = onNavigateBack,
        onPendingDraftConsumed = onPendingDraftConsumed,
        onPendingStartupAttachmentConsumed = onPendingStartupAttachmentConsumed,
    )

    ConversationScreenSurface(
        modifier = modifier,
        conversationId = conversationId,
        scaffoldUiState = scaffoldUiState,
        mediaPickerOverlayUiState = mediaPickerOverlayUiState,
        mediaPickerState = mediaPickerState,
        snackbarHostState = snackbarHostState,
        messageFieldFocusRequester = messageFieldFocusRequester,
        pendingScrollPosition = pendingScrollPosition,
        onPendingScrollPositionConsumed = onPendingScrollPositionConsumed,
        onAddPeopleClick = onAddPeopleClick,
        onConversationDetailsClick = onConversationDetailsClick,
        onNavigateBack = onNavigateBack,
        onHostBoundsChanged = { hostBounds ->
            hostBoundsState.value = hostBounds
        },
        onOpenContactPicker = onOpenContactPicker,
        onAudioRecordingStartRequest = {
            requestAudioRecordingStart(PendingAudioRecordingStartMode.Unlocked)
        },
        onLockedAudioRecordingStartRequest = {
            requestAudioRecordingStart(PendingAudioRecordingStartMode.Locked)
        },
        screenModel = screenModel,
    )
}

@Composable
internal fun ConversationScreenScaffold(
    modifier: Modifier = Modifier,
    conversationId: String?,
    uiState: ConversationScreenScaffoldUiState,
    snackbarHostState: SnackbarHostState,
    isMediaPickerOpen: Boolean,
    messageFieldFocusRequester: FocusRequester,
    pendingScrollPosition: Int?,
    onPendingScrollPositionConsumed: () -> Unit,
    onAddPeopleClick: () -> Unit,
    onConversationDetailsClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenContactPicker: () -> Unit,
    onOpenMediaPicker: () -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onLockedAudioRecordingStartRequest: () -> Unit,
    screenModel: ConversationScreenModel,
) {
    var isSimSheetVisible by rememberSaveable { mutableStateOf(value = false) }

    val hasSimSelector = uiState.composer.simSelector.isAvailable
    LaunchedEffect(hasSimSelector) {
        if (!hasSimSelector) {
            isSimSheetVisible = false
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ConversationScreenTopBar(
                uiState = uiState,
                onAddPeopleClick = onAddPeopleClick,
                onConversationDetailsClick = onConversationDetailsClick,
                onNavigateBack = onNavigateBack,
                onSimSelectorClick = { isSimSheetVisible = true },
                screenModel = screenModel,
            )
        },
        bottomBar = {
            ConversationScreenBottomBar(
                uiState = uiState,
                isMediaPickerOpen = isMediaPickerOpen,
                messageFieldFocusRequester = messageFieldFocusRequester,
                onOpenContactPicker = onOpenContactPicker,
                onOpenMediaPicker = onOpenMediaPicker,
                onAudioRecordingStartRequest = onAudioRecordingStartRequest,
                onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
                screenModel = screenModel,
            )
        },
    ) { contentPadding ->
        ConversationScreenContent(
            modifier = Modifier.fillMaxSize(),
            conversationId = conversationId,
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            contentPadding = contentPadding,
            pendingScrollPosition = pendingScrollPosition,
            onPendingScrollPositionConsumed = onPendingScrollPositionConsumed,
            onAttachmentClick = screenModel::onMessageAttachmentClicked,
            onExternalUriClick = screenModel::onExternalUriClicked,
            onMessageClick = screenModel::onMessageClick,
            onMessageLongClick = screenModel::onMessageLongClick,
            onMessageResendClick = screenModel::onMessageResendClick,
        )
    }

    ConversationScreenDialogs(uiState = uiState, screenModel = screenModel)

    ConversationScreenSimSelectorSheet(
        isVisible = isSimSheetVisible,
        uiState = uiState,
        onSimSelected = screenModel::onSimSelected,
        onDismissRequest = { isSimSheetVisible = false },
    )
}

@Composable
private fun ConversationScreenTopBar(
    uiState: ConversationScreenScaffoldUiState,
    onAddPeopleClick: () -> Unit,
    onConversationDetailsClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onSimSelectorClick: () -> Unit,
    screenModel: ConversationScreenModel,
) {
    when {
        uiState.selection.isSelectionMode -> {
            ConversationSelectionTopAppBar(
                selection = uiState.selection,
                onActionClick = screenModel::onMessageSelectionActionClick,
                onDismissSelection = screenModel::dismissMessageSelection,
            )
        }

        else -> {
            ConversationTopAppBar(
                metadata = uiState.metadata,
                isAddPeopleVisible = uiState.canAddPeople,
                isCallVisible = uiState.canCall,
                isArchiveVisible = uiState.canArchive,
                isUnarchiveVisible = uiState.canUnarchive,
                isAddContactVisible = uiState.canAddContact,
                isDeleteConversationVisible = uiState.canDeleteConversation,
                isShowSubjectFieldVisible = uiState.canEditSubject,
                simSelector = uiState.composer.simSelector,
                onAddPeopleClick = onAddPeopleClick,
                onCallClick = screenModel::onCallClick,
                onArchiveClick = screenModel::onArchiveConversationClick,
                onUnarchiveClick = screenModel::onUnarchiveConversationClick,
                onAddContactClick = screenModel::onAddContactClick,
                onDeleteConversationClick = screenModel::onDeleteConversationClick,
                onShowSubjectFieldClick = screenModel::onShowSubjectFieldClick,
                onSimSelectorClick = onSimSelectorClick,
                onTitleClick = onConversationDetailsClick,
                onNavigateBack = onNavigateBack,
            )
        }
    }
}

@Composable
private fun ConversationScreenBottomBar(
    uiState: ConversationScreenScaffoldUiState,
    isMediaPickerOpen: Boolean,
    messageFieldFocusRequester: FocusRequester,
    onOpenContactPicker: () -> Unit,
    onOpenMediaPicker: () -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onLockedAudioRecordingStartRequest: () -> Unit,
    screenModel: ConversationScreenModel,
) {
    if (isMediaPickerOpen) {
        return
    }

    ConversationComposerSection(
        audioRecording = uiState.composer.audioRecording,
        attachments = uiState.composer.attachments,
        messageText = uiState.composer.messageText,
        subjectText = uiState.composer.subjectText,
        sendProtocol = uiState.composer.sendProtocol,
        isMessageFieldEnabled = uiState.composer.isMessageFieldEnabled,
        isAttachmentActionEnabled = uiState.composer.isAttachmentActionEnabled,
        isRecordActionEnabled = uiState.composer.isRecordActionEnabled,
        isSendActionEnabled = uiState.composer.isSendEnabled,
        shouldShowRecordAction = uiState.composer.shouldShowRecordAction,
        messageFieldFocusRequester = messageFieldFocusRequester,
        onContactAttachClick = onOpenContactPicker,
        onMediaPickerClick = onOpenMediaPicker,
        onMessageTextChange = screenModel::onMessageTextChanged,
        onPendingAttachmentRemove = screenModel::onRemovePendingAttachment,
        onResolvedAttachmentClick = screenModel::onAttachmentClicked,
        onResolvedAttachmentRemove = screenModel::onRemoveResolvedAttachment,
        onAudioRecordingStartRequest = onAudioRecordingStartRequest,
        onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
        onAudioRecordingFinish = screenModel::onAudioRecordingFinish,
        onAudioRecordingLock = screenModel::onAudioRecordingLock,
        onAudioRecordingCancel = screenModel::onAudioRecordingCancel,
        onSendClick = screenModel::onSendClick,
        onSubjectChipClick = screenModel::onShowSubjectFieldClick,
        onSubjectChipClear = screenModel::onSubjectChipClear,
    )
}

@Composable
private fun ConversationScreenSimSelectorSheet(
    isVisible: Boolean,
    uiState: ConversationScreenScaffoldUiState,
    onSimSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (!isVisible || !uiState.composer.simSelector.isAvailable) {
        return
    }

    ConversationSimSelectorSheet(
        uiState = uiState.composer.simSelector,
        onSimSelected = { selfParticipantId ->
            onSimSelected(selfParticipantId)
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun ConversationScreenContent(
    modifier: Modifier = Modifier,
    conversationId: String?,
    uiState: ConversationScreenScaffoldUiState,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    pendingScrollPosition: Int?,
    onPendingScrollPositionConsumed: () -> Unit,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onMessageLongClick: (String) -> Unit,
    onMessageResendClick: (String) -> Unit,
) {
    when (val messagesState = uiState.messages) {
        is ConversationMessagesUiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues = contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG),
                )
            }
        }

        is ConversationMessagesUiState.Present -> {
            val messagesListState = rememberMessagesListState(
                conversationId = conversationId,
            )

            val showIncomingSenderLabels = shouldShowIncomingSenderLabels(
                metadata = uiState.metadata,
            )

            AutoScrollToLatestMessage(
                conversationId = conversationId,
                messages = messagesState.messages,
                listState = messagesListState,
                snackbarHostState = snackbarHostState,
            )

            ScrollToTargetMessage(
                conversationId = conversationId,
                pendingScrollPosition = pendingScrollPosition,
                messages = messagesState.messages,
                listState = messagesListState,
                onConsumed = onPendingScrollPositionConsumed,
            )

            ConversationMessages(
                modifier = modifier.padding(paddingValues = contentPadding),
                messages = messagesState.messages,
                listState = messagesListState,
                selectedMessageIds = uiState.selection.selectedMessageIds,
                showIncomingSenderLabels = showIncomingSenderLabels,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
                onMessageClick = onMessageClick,
                onMessageLongClick = onMessageLongClick,
                onMessageResendClick = onMessageResendClick,
            )
        }
    }
}

private fun shouldShowIncomingSenderLabels(metadata: ConversationMetadataUiState): Boolean {
    return when (metadata) {
        is ConversationMetadataUiState.Present -> metadata.participantCount > 1
        ConversationMetadataUiState.Loading,
        ConversationMetadataUiState.Unavailable,
        -> false
    }
}

@Composable
private fun AutoScrollToLatestMessage(
    conversationId: String?,
    messages: ImmutableList<ConversationMessageUiModel>,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
) {
    val latestMessage = messages.lastOrNull()
    val latestMessageId = latestMessage?.messageId
    val newMessageText = stringResource(id = R.string.in_conversation_notify_new_message_text)
    val viewActionLabel = stringResource(id = R.string.in_conversation_notify_new_message_action)

    var previousLatestMessageId by remember(conversationId) {
        mutableStateOf(value = latestMessageId)
    }

    var wasScrolledToLatestMessage by remember(
        conversationId,
        listState,
    ) {
        mutableStateOf(
            value = isScrolledToLatestMessage(listState = listState),
        )
    }

    LaunchedEffect(
        conversationId,
        listState,
    ) {
        snapshotFlow {
            isScrolledToLatestMessage(listState = listState)
        }.collect { isScrolledToLatestMessage ->
            wasScrolledToLatestMessage = isScrolledToLatestMessage
            if (isScrolledToLatestMessage) {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }

    LaunchedEffect(
        conversationId,
        latestMessageId,
    ) {
        val autoScrollDecision = evaluateConversationAutoScroll(
            input = ConversationAutoScrollInput(
                previousLatestMessageId = previousLatestMessageId,
                latestMessageId = latestMessageId,
                hasLatestMessage = latestMessage != null,
                isLatestMessageIncoming = latestMessage?.isIncoming ?: false,
                wasScrolledToLatestMessage = wasScrolledToLatestMessage,
            ),
        )

        previousLatestMessageId = autoScrollDecision.updatedLatestMessageId

        if (autoScrollDecision.shouldShowNewMessageSnackbar) {
            val snackbarResult = snackbarHostState.showSnackbar(
                message = newMessageText,
                actionLabel = viewActionLabel,
                duration = SnackbarDuration.Indefinite,
            )

            if (snackbarResult == SnackbarResult.ActionPerformed) {
                listState.animateScrollToItem(index = 0)
            }

            return@LaunchedEffect
        }

        if (!autoScrollDecision.shouldScrollToLatestMessage) {
            return@LaunchedEffect
        }

        listState.animateScrollToItem(index = 0)
    }
}

private fun isScrolledToLatestMessage(listState: LazyListState): Boolean {
    return listState.firstVisibleItemIndex == 0 &&
        listState.firstVisibleItemScrollOffset == 0
}

@Composable
private fun ScrollToTargetMessage(
    conversationId: String?,
    pendingScrollPosition: Int?,
    messages: ImmutableList<ConversationMessageUiModel>,
    listState: LazyListState,
    onConsumed: () -> Unit,
) {
    LaunchedEffect(
        conversationId,
        pendingScrollPosition,
        messages.size,
    ) {
        if (pendingScrollPosition == null || messages.isEmpty()) {
            return@LaunchedEffect
        }

        val displayIndex = messagePositionToDisplayIndex(
            position = pendingScrollPosition,
            size = messages.size,
        )

        val firstVisible = listState.firstVisibleItemIndex
        val delta = displayIndex - firstVisible

        val intermediateIndex = when {
            delta > SMOOTH_SCROLL_JUMP_THRESHOLD -> displayIndex - SMOOTH_SCROLL_JUMP_THRESHOLD
            delta < -SMOOTH_SCROLL_JUMP_THRESHOLD -> displayIndex + SMOOTH_SCROLL_JUMP_THRESHOLD
            else -> -1
        }

        if (intermediateIndex != -1) {
            listState.scrollToItem(index = intermediateIndex.coerceIn(0, messages.size - 1))
        }

        listState.animateScrollToItem(index = displayIndex)
        onConsumed()
    }
}

internal fun messagePositionToDisplayIndex(position: Int, size: Int): Int {
    return when {
        size <= 0 -> 0

        else -> {
            val lastIndex = size - 1
            (lastIndex - position).coerceIn(0, lastIndex)
        }
    }
}

@Composable
private fun rememberMessagesListState(
    conversationId: String?,
): LazyListState {
    return rememberSaveable(
        conversationId,
        saver = LazyListState.Saver,
    ) {
        LazyListState(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )
    }
}
