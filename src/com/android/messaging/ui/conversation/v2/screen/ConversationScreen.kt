package com.android.messaging.ui.conversation.v2.screen

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.ui.conversation.v2.CONVERSATION_LOADING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.composer.ui.ConversationComposerSection
import com.android.messaging.ui.conversation.v2.composer.ui.ConversationSimSelectorSheet
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationMediaPickerOverlay
import com.android.messaging.ui.conversation.v2.mediapicker.RefreshConversationMediaPickerPermissionsEffect
import com.android.messaging.ui.conversation.v2.mediapicker.rememberConversationMediaPickerPermissionState
import com.android.messaging.ui.conversation.v2.mediapicker.rememberConversationMediaPickerState
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.messages.ui.ConversationMessages
import com.android.messaging.ui.conversation.v2.metadata.ui.ConversationTopAppBar
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageDeleteConfirmationUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenScaffoldUiState
import kotlinx.collections.immutable.ImmutableList

private const val SMOOTH_SCROLL_JUMP_THRESHOLD = 15

private enum class PendingAudioRecordingStartMode {
    None,
    Unlocked,
    Locked,
}

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
    val messageFieldFocusRequester = remember {
        FocusRequester()
    }
    val mediaPickerState = rememberConversationMediaPickerState()
    val scaffoldUiState by screenModel.scaffoldUiState.collectAsStateWithLifecycle()
    val mediaPickerOverlayUiState by screenModel
        .mediaPickerOverlayUiState
        .collectAsStateWithLifecycle()

    val context = LocalContext.current
    val permissionState = rememberConversationMediaPickerPermissionState(context = context)

    val hostBoundsState = remember {
        mutableStateOf<ComposeRect?>(value = null)
    }

    var pendingAudioRecordingStartMode by rememberSaveable {
        mutableStateOf(value = PendingAudioRecordingStartMode.None)
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
    ) { contactUri ->
        screenModel.onContactCardPicked(contactUri = contactUri?.toString())
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        permissionState.audioPermissionGranted = isGranted

        val startMode = pendingAudioRecordingStartMode
        pendingAudioRecordingStartMode = PendingAudioRecordingStartMode.None

        if (!isGranted) {
            return@rememberLauncherForActivityResult
        }

        startAudioRecording(
            screenModel = screenModel,
            startMode = startMode,
        )
    }

    val requestAudioRecordingStart = { startMode: PendingAudioRecordingStartMode ->
        if (permissionState.audioPermissionGranted) {
            startAudioRecording(
                screenModel = screenModel,
                startMode = startMode,
            )
        } else {
            pendingAudioRecordingStartMode = startMode
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(conversationId) {
        screenModel.onConversationIdChanged(conversationId = conversationId)
    }

    LaunchedEffect(
        conversationId,
        launchGeneration,
        pendingDraft,
    ) {
        if (
            conversationId != null &&
            launchGeneration != null &&
            pendingDraft != null
        ) {
            screenModel.onSeedDraft(
                conversationId = conversationId,
                draft = pendingDraft,
            )
            onPendingDraftConsumed()
        }
    }

    LaunchedEffect(
        conversationId,
        launchGeneration,
        pendingStartupAttachment,
    ) {
        if (
            conversationId != null &&
            launchGeneration != null &&
            pendingStartupAttachment != null
        ) {
            screenModel.onOpenStartupAttachment(
                conversationId = conversationId,
                startupAttachment = pendingStartupAttachment,
            )
            onPendingStartupAttachmentConsumed()
        }
    }

    RefreshConversationMediaPickerPermissionsEffect(
        context = context,
        permissionState = permissionState,
    )

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        screenModel.onScreenForegrounded(cancelNotification = cancelIncomingNotification)
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_PAUSE) {
        screenModel.onScreenBackgrounded()
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_STOP) {
        val isRecording = scaffoldUiState.composer.audioRecording.phase ==
            ConversationAudioRecordingPhase.Recording

        if (isRecording) {
            screenModel.onAudioRecordingCancel()
        }
        screenModel.persistDraft()
    }

    BackHandler(enabled = scaffoldUiState.selection.isSelectionMode) {
        screenModel.dismissMessageSelection()
    }

    ConversationScreenEffects(
        screenModel = screenModel,
        hostBoundsState = hostBoundsState,
        onNavigateBack = onNavigateBack,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                hostBoundsState.value = coordinates.boundsInWindow()
            },
    ) {
        ConversationScreenScaffold(
            modifier = Modifier
                .fillMaxSize(),
            conversationId = conversationId,
            uiState = scaffoldUiState,
            isMediaPickerOpen = mediaPickerState.isOpen,
            messageFieldFocusRequester = messageFieldFocusRequester,
            pendingScrollPosition = pendingScrollPosition,
            onPendingScrollPositionConsumed = onPendingScrollPositionConsumed,
            onAddPeopleClick = onAddPeopleClick,
            onCallClick = screenModel::onCallClick,
            onConversationDetailsClick = onConversationDetailsClick,
            onNavigateBack = onNavigateBack,
            onArchiveConversationClick = screenModel::onArchiveConversationClick,
            onUnarchiveConversationClick = screenModel::onUnarchiveConversationClick,
            onAddContactClick = screenModel::onAddContactClick,
            onDeleteConversationClick = screenModel::onDeleteConversationClick,
            onDeleteConversationConfirmed = screenModel::confirmDeleteConversation,
            onDeleteConversationDismissed = screenModel::dismissDeleteConversationConfirmation,
            onDeleteSelectedMessagesConfirmed = screenModel::confirmDeleteSelectedMessages,
            onDeleteSelectedMessagesDismissed = screenModel::dismissDeleteMessageConfirmation,
            onDismissMessageSelection = screenModel::dismissMessageSelection,
            onMessageClick = screenModel::onMessageClick,
            onMessageLongClick = screenModel::onMessageLongClick,
            onMessageSelectionActionClick = screenModel::onMessageSelectionActionClick,
            onOpenContactPicker = {
                contactPickerLauncher.launch(input = null)
            },
            onOpenMediaPicker = mediaPickerState::open,
            onMessageTextChange = screenModel::onMessageTextChanged,
            onPendingAttachmentRemove = screenModel::onRemovePendingAttachment,
            onResolvedAttachmentClick = screenModel::onAttachmentClicked,
            onResolvedAttachmentRemove = screenModel::onRemoveResolvedAttachment,
            onAudioRecordingStartRequest = {
                requestAudioRecordingStart(PendingAudioRecordingStartMode.Unlocked)
            },
            onLockedAudioRecordingStartRequest = {
                requestAudioRecordingStart(PendingAudioRecordingStartMode.Locked)
            },
            onAudioRecordingFinish = screenModel::onAudioRecordingFinish,
            onAudioRecordingLock = screenModel::onAudioRecordingLock,
            onAudioRecordingCancel = screenModel::onAudioRecordingCancel,
            onSendClick = screenModel::onSendClick,
            onSimSelected = screenModel::onSimSelected,
            onAttachmentClick = screenModel::onMessageAttachmentClicked,
            onExternalUriClick = screenModel::onExternalUriClicked,
        )

        ConversationMediaPickerOverlay(
            modifier = Modifier
                .fillMaxSize(),
            state = mediaPickerState,
            mediaPickerUiState = mediaPickerOverlayUiState.mediaPicker,
            attachments = mediaPickerOverlayUiState.attachments,
            conversationTitle = mediaPickerOverlayUiState.conversationTitle,
            isSendActionEnabled = mediaPickerOverlayUiState.isSendActionEnabled,
            messageFieldFocusRequester = messageFieldFocusRequester,
            onAttachmentPreviewClick = { attachment ->
                screenModel.onAttachmentClicked(attachment = attachment)
            },
            onAttachmentCaptionChange = screenModel::onUpdateAttachmentCaption,
            onAttachmentRemove = screenModel::onRemoveResolvedAttachment,
            onGalleryMediaConfirmed = screenModel::onGalleryMediaConfirmed,
            onGalleryVisibilityChanged = screenModel::onGalleryVisibilityChanged,
            onCapturedMediaReady = screenModel::onCapturedMediaReady,
            onSendClick = screenModel::onSendClick,
        )
    }
}

private fun startAudioRecording(
    screenModel: ConversationScreenModel,
    startMode: PendingAudioRecordingStartMode,
) {
    when (startMode) {
        PendingAudioRecordingStartMode.None -> Unit
        PendingAudioRecordingStartMode.Unlocked -> screenModel.onAudioRecordingStart()
        PendingAudioRecordingStartMode.Locked -> screenModel.onLockedAudioRecordingStart()
    }
}

@Composable
private fun ConversationScreenScaffold(
    modifier: Modifier = Modifier,
    conversationId: String?,
    uiState: ConversationScreenScaffoldUiState,
    isMediaPickerOpen: Boolean,
    messageFieldFocusRequester: FocusRequester,
    pendingScrollPosition: Int?,
    onPendingScrollPositionConsumed: () -> Unit,
    onAddPeopleClick: () -> Unit,
    onCallClick: () -> Unit,
    onConversationDetailsClick: () -> Unit,
    onArchiveConversationClick: () -> Unit,
    onUnarchiveConversationClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onDeleteConversationClick: () -> Unit,
    onDeleteConversationConfirmed: () -> Unit,
    onDeleteConversationDismissed: () -> Unit,
    onDeleteSelectedMessagesConfirmed: () -> Unit,
    onDeleteSelectedMessagesDismissed: () -> Unit,
    onDismissMessageSelection: () -> Unit,
    onMessageClick: (String) -> Unit,
    onMessageLongClick: (String) -> Unit,
    onMessageSelectionActionClick: (ConversationMessageSelectionAction) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenContactPicker: () -> Unit,
    onOpenMediaPicker: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onPendingAttachmentRemove: (String) -> Unit,
    onResolvedAttachmentClick: (ComposerAttachmentUiModel.Resolved) -> Unit,
    onResolvedAttachmentRemove: (String) -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onLockedAudioRecordingStartRequest: () -> Unit,
    onAudioRecordingFinish: () -> Unit,
    onAudioRecordingLock: () -> Boolean,
    onAudioRecordingCancel: () -> Unit,
    onSendClick: () -> Unit,
    onSimSelected: (String) -> Unit,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
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
        topBar = {
            when {
                uiState.selection.isSelectionMode -> {
                    ConversationSelectionTopAppBar(
                        selection = uiState.selection,
                        onActionClick = onMessageSelectionActionClick,
                        onDismissSelection = onDismissMessageSelection,
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
                        simSelector = uiState.composer.simSelector,
                        onAddPeopleClick = onAddPeopleClick,
                        onCallClick = onCallClick,
                        onArchiveClick = onArchiveConversationClick,
                        onUnarchiveClick = onUnarchiveConversationClick,
                        onAddContactClick = onAddContactClick,
                        onDeleteConversationClick = onDeleteConversationClick,
                        onSimSelectorClick = { isSimSheetVisible = true },
                        onTitleClick = onConversationDetailsClick,
                        onNavigateBack = onNavigateBack,
                    )
                }
            }
        },
        bottomBar = {
            if (!isMediaPickerOpen) {
                ConversationComposerSection(
                    audioRecording = uiState.composer.audioRecording,
                    attachments = uiState.composer.attachments,
                    messageText = uiState.composer.messageText,
                    isMessageFieldEnabled = uiState.composer.isMessageFieldEnabled,
                    isAttachmentActionEnabled = uiState.composer.isAttachmentActionEnabled,
                    isRecordActionEnabled = uiState.composer.isRecordActionEnabled,
                    isSendActionEnabled = uiState.composer.isSendEnabled,
                    shouldShowRecordAction = uiState.composer.shouldShowRecordAction,
                    messageFieldFocusRequester = messageFieldFocusRequester,
                    onContactAttachClick = onOpenContactPicker,
                    onMediaPickerClick = onOpenMediaPicker,
                    onMessageTextChange = onMessageTextChange,
                    onPendingAttachmentRemove = onPendingAttachmentRemove,
                    onResolvedAttachmentClick = onResolvedAttachmentClick,
                    onResolvedAttachmentRemove = onResolvedAttachmentRemove,
                    onAudioRecordingStartRequest = onAudioRecordingStartRequest,
                    onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
                    onAudioRecordingFinish = onAudioRecordingFinish,
                    onAudioRecordingLock = onAudioRecordingLock,
                    onAudioRecordingCancel = onAudioRecordingCancel,
                    onSendClick = onSendClick,
                )
            }
        },
    ) { contentPadding ->
        ConversationScreenContent(
            modifier = Modifier.fillMaxSize(),
            conversationId = conversationId,
            uiState = uiState,
            contentPadding = contentPadding,
            pendingScrollPosition = pendingScrollPosition,
            onPendingScrollPositionConsumed = onPendingScrollPositionConsumed,
            onAttachmentClick = onAttachmentClick,
            onExternalUriClick = onExternalUriClick,
            onMessageClick = onMessageClick,
            onMessageLongClick = onMessageLongClick,
        )
    }

    uiState.selection.deleteConfirmation?.let { deleteConfirmation ->
        ConversationDeleteMessagesDialog(
            deleteConfirmation = deleteConfirmation,
            onConfirm = onDeleteSelectedMessagesConfirmed,
            onDismiss = onDeleteSelectedMessagesDismissed,
        )
    }

    if (uiState.isDeleteConversationConfirmationVisible) {
        ConversationDeleteConversationDialog(
            onConfirm = onDeleteConversationConfirmed,
            onDismiss = onDeleteConversationDismissed,
        )
    }

    if (isSimSheetVisible && hasSimSelector) {
        ConversationSimSelectorSheet(
            uiState = uiState.composer.simSelector,
            onSimSelected = { selfParticipantId ->
                onSimSelected(selfParticipantId)
                isSimSheetVisible = false
            },
            onDismissRequest = {
                isSimSheetVisible = false
            },
        )
    }
}

@Composable
private fun ConversationDeleteConversationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = pluralStringResource(
                    id = R.plurals.delete_conversations_confirmation_dialog_title,
                    count = 1,
                    1,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.delete_conversation_confirmation_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.delete_conversation_decline_button))
            }
        },
    )
}

@Composable
private fun ConversationScreenContent(
    modifier: Modifier = Modifier,
    conversationId: String?,
    uiState: ConversationScreenScaffoldUiState,
    contentPadding: PaddingValues,
    pendingScrollPosition: Int?,
    onPendingScrollPositionConsumed: () -> Unit,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onMessageLongClick: (String) -> Unit,
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

            AutoScrollToLatestMessage(
                conversationId = conversationId,
                messages = messagesState.messages,
                listState = messagesListState,
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
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
                onMessageClick = onMessageClick,
                onMessageLongClick = onMessageLongClick,
            )
        }
    }
}

@Composable
private fun ConversationDeleteMessagesDialog(
    deleteConfirmation: ConversationMessageDeleteConfirmationUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = pluralStringResource(
                    id = R.plurals.delete_messages_confirmation_dialog_title,
                    count = deleteConfirmation.messageIds.size,
                    deleteConfirmation.messageIds.size,
                ),
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_message_confirmation_dialog_text),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(
                    text = stringResource(R.string.delete_message_confirmation_button),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = stringResource(android.R.string.cancel),
                )
            }
        },
    )
}

@Composable
private fun AutoScrollToLatestMessage(
    conversationId: String?,
    messages: ImmutableList<ConversationMessageUiModel>,
    listState: LazyListState,
) {
    val latestMessage = messages.lastOrNull()
    val latestMessageId = latestMessage?.messageId

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
