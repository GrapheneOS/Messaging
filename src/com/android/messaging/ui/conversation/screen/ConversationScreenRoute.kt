package com.android.messaging.ui.conversation.screen

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.mediapicker.ConversationMediaPickerOverlay
import com.android.messaging.ui.conversation.mediapicker.ConversationMediaPickerState
import com.android.messaging.ui.conversation.mediapicker.RefreshConversationMediaPickerPermissionsEffect
import com.android.messaging.ui.conversation.mediapicker.model.ConversationMediaPickerPermissionState
import com.android.messaging.ui.conversation.screen.model.ConversationMediaPickerOverlayUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState

@Composable
internal fun rememberOpenContactPickerCallback(
    screenModel: ConversationScreenModel,
): () -> Unit {
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
    ) { contactUri ->
        screenModel.onContactCardPicked(contactUri = contactUri?.toString())
    }

    return remember(contactPickerLauncher) {
        {
            contactPickerLauncher.launch(input = null)
        }
    }
}

@Composable
internal fun rememberAudioRecordingStartRequest(
    screenModel: ConversationScreenModel,
    permissionState: ConversationMediaPickerPermissionState,
): (PendingAudioRecordingStartMode) -> Unit {
    var pendingAudioRecordingStartMode by rememberSaveable {
        mutableStateOf(value = PendingAudioRecordingStartMode.None)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        permissionState.audioPermissionGranted = isGranted

        val startMode = pendingAudioRecordingStartMode
        pendingAudioRecordingStartMode = PendingAudioRecordingStartMode.None

        if (isGranted) {
            startAudioRecording(
                screenModel = screenModel,
                startMode = startMode,
            )
        }
    }

    return remember(screenModel, permissionState, audioPermissionLauncher) {
        { startMode ->
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
    }
}

@Composable
internal fun ConversationScreenRouteEffects(
    conversationId: String?,
    launchGeneration: Int?,
    cancelIncomingNotification: Boolean,
    pendingDraft: ConversationDraft?,
    pendingStartupAttachment: ConversationEntryStartupAttachment?,
    scaffoldUiState: ConversationScreenScaffoldUiState,
    snackbarHostState: SnackbarHostState,
    hostBoundsState: State<ComposeRect?>,
    permissionState: ConversationMediaPickerPermissionState,
    screenModel: ConversationScreenModel,
    onNavigateBack: () -> Unit,
    onPendingDraftConsumed: () -> Unit,
    onPendingStartupAttachmentConsumed: () -> Unit,
) {
    ConversationPendingLaunchEffects(
        conversationId = conversationId,
        launchGeneration = launchGeneration,
        pendingDraft = pendingDraft,
        pendingStartupAttachment = pendingStartupAttachment,
        screenModel = screenModel,
        onPendingDraftConsumed = onPendingDraftConsumed,
        onPendingStartupAttachmentConsumed = onPendingStartupAttachmentConsumed,
    )

    RefreshConversationMediaPickerPermissionsEffect(
        permissionState = permissionState,
    )

    ConversationScreenLifecycleEffects(
        cancelIncomingNotification = cancelIncomingNotification,
        uiState = scaffoldUiState,
        screenModel = screenModel,
    )

    BackHandler(enabled = scaffoldUiState.selection.isSelectionMode) {
        screenModel.dismissMessageSelection()
    }

    ConversationScreenEffects(
        screenModel = screenModel,
        snackbarHostState = snackbarHostState,
        hostBoundsState = hostBoundsState,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
private fun ConversationPendingLaunchEffects(
    conversationId: String?,
    launchGeneration: Int?,
    pendingDraft: ConversationDraft?,
    pendingStartupAttachment: ConversationEntryStartupAttachment?,
    screenModel: ConversationScreenModel,
    onPendingDraftConsumed: () -> Unit,
    onPendingStartupAttachmentConsumed: () -> Unit,
) {
    LaunchedEffect(conversationId, screenModel) {
        screenModel.onConversationIdChanged(conversationId = conversationId)
    }

    LaunchedEffect(conversationId, launchGeneration, pendingDraft, screenModel) {
        if (conversationId != null && launchGeneration != null && pendingDraft != null) {
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
        screenModel,
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
}

@Composable
private fun ConversationScreenLifecycleEffects(
    cancelIncomingNotification: Boolean,
    uiState: ConversationScreenScaffoldUiState,
    screenModel: ConversationScreenModel,
) {
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        screenModel.onScreenForegrounded(cancelNotification = cancelIncomingNotification)
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_PAUSE) {
        screenModel.onScreenBackgrounded()
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_STOP) {
        val isRecording = uiState.composer.audioRecording.phase ==
            ConversationAudioRecordingPhase.Recording

        if (isRecording) {
            screenModel.onAudioRecordingCancel()
        }
        screenModel.persistDraft()
    }
}

@Composable
internal fun ConversationScreenSurface(
    modifier: Modifier,
    conversationId: String?,
    scaffoldUiState: ConversationScreenScaffoldUiState,
    mediaPickerOverlayUiState: ConversationMediaPickerOverlayUiState,
    mediaPickerState: ConversationMediaPickerState,
    snackbarHostState: SnackbarHostState,
    messageFieldFocusRequester: FocusRequester,
    pendingScrollPosition: Int?,
    onPendingScrollPositionConsumed: () -> Unit,
    onAddPeopleClick: () -> Unit,
    onConversationDetailsClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onHostBoundsChanged: (ComposeRect) -> Unit,
    onOpenContactPicker: () -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onLockedAudioRecordingStartRequest: () -> Unit,
    screenModel: ConversationScreenModel,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                onHostBoundsChanged(coordinates.boundsInWindow())
            },
    ) {
        ConversationScreenScaffold(
            modifier = Modifier.fillMaxSize(),
            conversationId = conversationId,
            uiState = scaffoldUiState,
            snackbarHostState = snackbarHostState,
            isMediaPickerOpen = mediaPickerState.isOpen,
            messageFieldFocusRequester = messageFieldFocusRequester,
            pendingScrollPosition = pendingScrollPosition,
            onPendingScrollPositionConsumed = onPendingScrollPositionConsumed,
            onAddPeopleClick = onAddPeopleClick,
            onConversationDetailsClick = onConversationDetailsClick,
            onNavigateBack = onNavigateBack,
            onOpenContactPicker = onOpenContactPicker,
            onOpenMediaPicker = mediaPickerState::open,
            onAudioRecordingStartRequest = onAudioRecordingStartRequest,
            onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
            screenModel = screenModel,
        )

        ConversationMediaPickerOverlayHost(
            modifier = Modifier.fillMaxSize(),
            uiState = mediaPickerOverlayUiState,
            state = mediaPickerState,
            messageFieldFocusRequester = messageFieldFocusRequester,
            screenModel = screenModel,
        )
    }
}

@Composable
private fun ConversationMediaPickerOverlayHost(
    modifier: Modifier,
    uiState: ConversationMediaPickerOverlayUiState,
    state: ConversationMediaPickerState,
    messageFieldFocusRequester: FocusRequester,
    screenModel: ConversationScreenModel,
) {
    ConversationMediaPickerOverlay(
        modifier = modifier,
        state = state,
        attachments = uiState.attachments,
        conversationTitle = uiState.conversationTitle,
        isSendActionEnabled = uiState.isSendActionEnabled,
        messageFieldFocusRequester = messageFieldFocusRequester,
        onAttachmentPreviewClick = { attachment ->
            screenModel.onAttachmentClicked(attachment = attachment)
        },
        onAttachmentCaptionChange = screenModel::onUpdateAttachmentCaption,
        onAttachmentRemove = screenModel::onRemoveResolvedAttachment,
        photoPickerSourceContentUriByAttachmentContentUri =
            uiState.photoPickerSourceContentUriByAttachmentContentUri,
        onPhotoPickerMediaSelected = screenModel::onPhotoPickerMediaSelected,
        onPhotoPickerMediaDeselected = screenModel::onPhotoPickerMediaDeselected,
        onCapturedMediaReady = screenModel::onCapturedMediaReady,
        onSendClick = screenModel::onSendClick,
    )
}

private fun startAudioRecording(
    screenModel: ConversationScreenModel,
    startMode: PendingAudioRecordingStartMode,
) {
    when (startMode) {
        PendingAudioRecordingStartMode.None -> {}

        PendingAudioRecordingStartMode.Unlocked -> {
            screenModel.onAudioRecordingStart()
        }

        PendingAudioRecordingStartMode.Locked -> {
            screenModel.onLockedAudioRecordingStart()
        }
    }
}
