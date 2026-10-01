package com.android.messaging.ui.conversation.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.android.messaging.R
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.common.components.snackbar.showActionSnackbar
import com.android.messaging.ui.contact.model.AddContactRequest
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.mediapicker.ConversationMediaPickerOverlay
import com.android.messaging.ui.conversation.mediapicker.ConversationMediaPickerState
import com.android.messaging.ui.conversation.mediapicker.RefreshConversationMediaPickerPermissionsEffect
import com.android.messaging.ui.conversation.mediapicker.model.ConversationMediaPickerPermissionState
import com.android.messaging.ui.conversation.screen.model.ConversationMediaPickerOverlayUiState
import com.android.messaging.ui.conversation.screen.model.ConversationPendingLaunchPayload
import com.android.messaging.ui.conversation.screen.model.ConversationScreenNavEvent
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.core.CollectEvents
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequest

@Composable
internal fun rememberOpenContactPickerCallback(
    screenModel: ConversationScreenModel,
): () -> Unit {
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
    ) { contactUri ->
        screenModel.onContactCardPicked(contactUri = contactUri?.toString())
    }

    return remember(screenModel, contactPickerLauncher) {
        {
            if (screenModel.tryStartAddingAttachment()) {
                contactPickerLauncher.launch(input = null)
            }
        }
    }
}

@Composable
internal fun rememberAudioRecordingStartRequest(
    screenModel: ConversationScreenModel,
    permissionState: ConversationMediaPickerPermissionState,
): (AudioRecordingStartMode) -> Unit {
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        permissionState.audioPermissionGranted = isGranted
    }

    return remember(screenModel, permissionState, audioPermissionLauncher) {
        { startMode ->
            val canStartAddingAttachment = screenModel.tryStartAddingAttachment()

            when {
                !canStartAddingAttachment -> Unit

                permissionState.audioPermissionGranted -> {
                    startAudioRecording(
                        screenModel = screenModel,
                        startMode = startMode,
                    )
                }

                else -> {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }
}

@Composable
internal fun ConversationScreenRouteEffects(
    conversationId: ConversationId?,
    cancelIncomingNotification: Boolean,
    offersArchiveUndo: Boolean,
    pendingLaunchPayload: ConversationPendingLaunchPayload,
    scaffoldUiState: ConversationScreenScaffoldUiState,
    snackbarHostState: SnackbarHostState,
    hostBoundsState: State<ComposeRect?>,
    permissionState: ConversationMediaPickerPermissionState,
    screenModel: ConversationScreenModel,
    onNavigateToMessageDetails: (messageId: MessageId) -> Unit,
    onNavigateToVCardDetail: (uri: String) -> Unit,
    onNavigateToPhotoViewer: (PhotoViewerLaunchRequest) -> Unit,
    onNavigateToAddContact: (AddContactRequest) -> Unit,
    onNavigateToForward: (messageId: MessageId) -> Unit,
    onCloseConversation: () -> Unit,
    onPendingDraftConsumed: () -> Unit,
    onPendingSelfParticipantIdConsumed: () -> Unit,
    onPendingStartupAttachmentConsumed: () -> Unit,
) {
    ConversationPendingLaunchEffects(
        conversationId = conversationId,
        pendingLaunchPayload = pendingLaunchPayload,
        screenModel = screenModel,
        onPendingDraftConsumed = onPendingDraftConsumed,
        onPendingSelfParticipantIdConsumed = onPendingSelfParticipantIdConsumed,
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

    CollectEvents(events = screenModel.navigationEvents) { event ->
        when (event) {
            ConversationScreenNavEvent.CloseConversation -> onCloseConversation()

            ConversationScreenNavEvent.CloseAfterArchive -> {
                when {
                    offersArchiveUndo -> Unit
                    else -> onCloseConversation()
                }
            }

            is ConversationScreenNavEvent.NavigateToMessageDetails -> {
                onNavigateToMessageDetails(event.messageId)
            }

            is ConversationScreenNavEvent.ForwardMessage -> {
                onNavigateToForward(event.messageId)
            }
        }
    }

    if (offersArchiveUndo) {
        ConversationArchiveUndoEffect(
            screenModel = screenModel,
            snackbarHostState = snackbarHostState,
        )
    }

    ConversationScreenEffects(
        screenModel = screenModel,
        snackbarHostState = snackbarHostState,
        hostBoundsState = hostBoundsState,
        onNavigateToVCardDetail = onNavigateToVCardDetail,
        onNavigateToPhotoViewer = onNavigateToPhotoViewer,
        onNavigateToAddContact = onNavigateToAddContact,
    )
}

// Without an inbox to return to, archiving keeps the conversation open and offers Undo here.
@Composable
private fun ConversationArchiveUndoEffect(
    screenModel: ConversationScreenModel,
    snackbarHostState: SnackbarHostState,
) {
    val resources = LocalResources.current

    CollectEvents(events = screenModel.archivedConversationIds) { conversationId ->
        // An Indefinite notice (new message) would otherwise hold Undo back.
        snackbarHostState.currentSnackbarData?.dismiss()

        val undoClicked = snackbarHostState.showActionSnackbar(
            message = resources.getString(R.string.archived_toast_message, 1),
            actionLabel = resources.getString(R.string.snack_bar_undo),
        )

        if (undoClicked) {
            screenModel.onUndoArchiveClick(conversationId = conversationId)
        }
    }
}

@Composable
private fun ConversationPendingLaunchEffects(
    conversationId: ConversationId?,
    pendingLaunchPayload: ConversationPendingLaunchPayload,
    screenModel: ConversationScreenModel,
    onPendingDraftConsumed: () -> Unit,
    onPendingSelfParticipantIdConsumed: () -> Unit,
    onPendingStartupAttachmentConsumed: () -> Unit,
) {
    val pendingDraft = pendingLaunchPayload.draft
    val pendingSelfParticipantId = pendingLaunchPayload.selfParticipantId
    val pendingStartupAttachment = pendingLaunchPayload.startupAttachment

    LaunchedEffect(conversationId, screenModel) {
        screenModel.onConversationIdChanged(conversationId = conversationId)
    }

    LaunchedEffect(conversationId, pendingDraft, screenModel) {
        if (conversationId != null && pendingDraft != null) {
            screenModel.onSeedDraft(
                conversationId = conversationId,
                draft = pendingDraft,
            )
            onPendingDraftConsumed()
        }
    }

    LaunchedEffect(
        conversationId,
        pendingSelfParticipantId,
        screenModel,
    ) {
        if (
            conversationId != null &&
            pendingSelfParticipantId != null
        ) {
            screenModel.onSimSelected(selfParticipantId = pendingSelfParticipantId)
            onPendingSelfParticipantIdConsumed()
        }
    }

    LaunchedEffect(
        conversationId,
        pendingStartupAttachment,
        screenModel,
    ) {
        if (
            conversationId != null &&
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
    LifecycleResumeEffect(screenModel, cancelIncomingNotification) {
        screenModel.onScreenForegrounded(cancelNotification = cancelIncomingNotification)
        onPauseOrDispose { screenModel.onScreenBackgrounded() }
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
    conversationId: ConversationId?,
    scaffoldUiState: ConversationScreenScaffoldUiState,
    mediaPickerOverlayUiState: ConversationMediaPickerOverlayUiState,
    mediaPickerState: ConversationMediaPickerState,
    snackbarHostState: SnackbarHostState,
    messageFieldFocusRequester: FocusRequester,
    pendingScrollMessageId: MessageId?,
    onPendingScrollMessageIdConsumed: () -> Unit,
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
            pendingScrollMessageId = pendingScrollMessageId,
            onPendingScrollMessageIdConsumed = onPendingScrollMessageIdConsumed,
            onAddPeopleClick = onAddPeopleClick,
            onConversationDetailsClick = onConversationDetailsClick,
            onNavigateBack = onNavigateBack,
            onOpenContactPicker = onOpenContactPicker,
            onOpenMediaPicker = mediaPickerState::open,
            onAudioRecordingStartRequest = onAudioRecordingStartRequest,
            onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
            screenModel = screenModel,
        )

        // "Call requires version 15 of the U Extensions SDK" is OK in this case: all GrapheneOS
        // users will have this version
        ConversationMediaPickerOverlayHost(
            modifier = Modifier.fillMaxSize(),
            uiState = mediaPickerOverlayUiState,
            state = mediaPickerState,
            messageFieldFocusRequester = messageFieldFocusRequester,
            screenModel = screenModel,
        )
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
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
        onAttachmentStartRequest = screenModel::tryStartAddingAttachment,
        onCapturedMediaReady = screenModel::onCapturedMediaReady,
        onSendClick = screenModel::onSendClick,
    )
}

private fun startAudioRecording(
    screenModel: ConversationScreenModel,
    startMode: AudioRecordingStartMode,
) {
    screenModel.onAudioRecordingStart(
        isLocked = startMode == AudioRecordingStartMode.Locked,
    )
}
