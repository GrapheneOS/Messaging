package com.android.messaging.ui.conversation.v2.mediapicker

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.ui.conversation.v2.CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerAttachmentUiState
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationMediaPickerUiState
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConversationMediaPickerOverlay(
    modifier: Modifier = Modifier,
    state: ConversationMediaPickerState,
    mediaPickerUiState: ConversationMediaPickerUiState,
    attachments: ImmutableList<ConversationComposerAttachmentUiState>,
    conversationTitle: String?,
    isSendActionEnabled: Boolean,
    messageFieldFocusRequester: FocusRequester,
    onAttachmentPreviewClick: (ConversationComposerAttachmentUiState.Resolved) -> Unit,
    onAttachmentCaptionChange: (String, String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    onGalleryMediaConfirmed: (List<ConversationMediaItem>) -> Unit,
    onGalleryVisibilityChanged: (Boolean) -> Unit,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onSendClick: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current

    val permissionState = rememberConversationMediaPickerPermissionState(context = context)

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        permissionState.audioPermissionGranted = isGranted
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        permissionState.cameraPermissionGranted = isGranted
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissionResults ->
        permissionState.galleryPermissionGranted = permissionResults.values.all { isGranted ->
            isGranted
        }
    }

    HandleConversationMediaPickerGalleryVisibilityEffect(
        state = state,
        galleryPermissionGranted = permissionState.galleryPermissionGranted,
        onGalleryVisibilityChanged = onGalleryVisibilityChanged,
    )

    HandleConversationMediaPickerVisibilityEffect(
        state = state,
        isImeVisible = isImeVisible,
        focusManager = focusManager,
        keyboardController = keyboardController,
        messageFieldFocusRequester = messageFieldFocusRequester,
    )

    RefreshConversationMediaPickerPermissionsEffect(
        context = context,
        permissionState = permissionState,
    )

    BackHandler(enabled = state.isOpen) {
        state.close()
    }

    if (!state.isOpen) {
        return
    }

    ConversationMediaPicker(
        modifier = modifier
            .fillMaxSize()
            .testTag(CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG),
        uiState = mediaPickerUiState,
        attachments = attachments,
        conversationTitle = conversationTitle,
        isSendActionEnabled = isSendActionEnabled,
        state = state,
        cameraPermissionGranted = permissionState.cameraPermissionGranted,
        audioPermissionGranted = permissionState.audioPermissionGranted,
        galleryPermissionGranted = permissionState.galleryPermissionGranted,
        onClose = state::close,
        onAttachmentPreviewClick = onAttachmentPreviewClick,
        onAttachmentCaptionChange = onAttachmentCaptionChange,
        onAttachmentRemove = onAttachmentRemove,
        onGalleryMediaConfirmed = onGalleryMediaConfirmed,
        onRequestAudioPermission = {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onRequestCameraPermission = {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onRequestGalleryPermission = {
            galleryPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                ),
            )
        },
        onCapturedMediaReady = onCapturedMediaReady,
        onSendClick = onSendClick,
    )
}
