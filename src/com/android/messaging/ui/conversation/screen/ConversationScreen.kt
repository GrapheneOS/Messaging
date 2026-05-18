package com.android.messaging.ui.conversation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.ui.conversation.composer.ui.ConversationComposerSection
import com.android.messaging.ui.conversation.composer.ui.ConversationSimSelectorSheet
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.mediapicker.rememberConversationMediaPickerPermissionState
import com.android.messaging.ui.conversation.mediapicker.rememberConversationMediaPickerState
import com.android.messaging.ui.conversation.metadata.ui.ConversationTopAppBar
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState

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
    pendingSelfParticipantId: String? = null,
    pendingStartupAttachment: ConversationEntryStartupAttachment? = null,
    onPendingDraftConsumed: () -> Unit = {},
    onPendingScrollPositionConsumed: () -> Unit = {},
    onPendingSelfParticipantIdConsumed: () -> Unit = {},
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
        pendingSelfParticipantId = pendingSelfParticipantId,
        pendingStartupAttachment = pendingStartupAttachment,
        scaffoldUiState = scaffoldUiState,
        snackbarHostState = snackbarHostState,
        hostBoundsState = hostBoundsState,
        permissionState = permissionState,
        screenModel = screenModel,
        onNavigateBack = onNavigateBack,
        onPendingDraftConsumed = onPendingDraftConsumed,
        onPendingSelfParticipantIdConsumed = onPendingSelfParticipantIdConsumed,
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
            requestAudioRecordingStart(AudioRecordingStartMode.Unlocked)
        },
        onLockedAudioRecordingStartRequest = {
            requestAudioRecordingStart(AudioRecordingStartMode.Locked)
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
    val simSheetState = rememberConversationSimSheetState(
        isAvailable = uiState.composer.simSelector.isAvailable,
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ConversationScreenTopBar(
                uiState = uiState,
                onAddPeopleClick = onAddPeopleClick,
                onConversationDetailsClick = onConversationDetailsClick,
                onNavigateBack = onNavigateBack,
                onSimSelectorClick = simSheetState::show,
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
                onSendActionLongClick = simSheetState::show,
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
            onMessageAvatarClick = screenModel::onMessageAvatarClick,
            onMessageDownloadClick = screenModel::onMessageDownloadClick,
            onMessageLongClick = screenModel::onMessageLongClick,
            onMessageResendClick = screenModel::onMessageResendClick,
            onSimSelectorClick = simSheetState::show,
        )
    }

    ConversationScreenDialogs(uiState = uiState, screenModel = screenModel)

    ConversationScreenSimSelectorSheet(
        simSheetState = simSheetState,
        uiState = uiState,
        onSimSelected = screenModel::onSimSelected,
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
    onSendActionLongClick: () -> Unit,
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
        segmentCounter = uiState.composer.segmentCounter,
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
        onSendActionLongClick = onSendActionLongClick,
        onSubjectChipClick = screenModel::onShowSubjectFieldClick,
        onSubjectChipClear = screenModel::onSubjectChipClear,
    )
}

@Composable
private fun ConversationScreenSimSelectorSheet(
    simSheetState: ConversationSimSheetState,
    uiState: ConversationScreenScaffoldUiState,
    onSimSelected: (String) -> Unit,
) {
    if (!simSheetState.isVisible || !uiState.composer.simSelector.isAvailable) {
        return
    }

    ConversationSimSelectorSheet(
        uiState = uiState.composer.simSelector,
        onSimSelected = { selfParticipantId ->
            onSimSelected(selfParticipantId)
            simSheetState.dismiss()
        },
        onDismissRequest = simSheetState::dismiss,
    )
}
