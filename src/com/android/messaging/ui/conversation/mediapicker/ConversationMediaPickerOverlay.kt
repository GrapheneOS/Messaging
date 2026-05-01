package com.android.messaging.ui.conversation.mediapicker

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import com.android.messaging.data.media.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConversationMediaPickerOverlay(
    modifier: Modifier = Modifier,
    state: ConversationMediaPickerState,
    attachments: ImmutableList<ComposerAttachmentUiModel>,
    conversationTitle: String?,
    isSendActionEnabled: Boolean,
    messageFieldFocusRequester: FocusRequester,
    onAttachmentPreviewClick: (ComposerAttachmentUiModel.Resolved.VisualMedia) -> Unit,
    onAttachmentCaptionChange: (String, String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    photoPickerSourceContentUriByAttachmentContentUri: ImmutableMap<String, String>,
    onPhotoPickerMediaSelected: (List<String>) -> Unit,
    onPhotoPickerMediaDeselected: (List<String>) -> Unit,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onSendClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current

    val permissionState = rememberConversationMediaPickerPermissionState()

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

    HandleConversationMediaPickerVisibilityEffect(
        state = state,
        isImeVisible = isImeVisible,
        focusManager = focusManager,
        keyboardController = keyboardController,
        messageFieldFocusRequester = messageFieldFocusRequester,
    )

    RefreshConversationMediaPickerPermissionsEffect(
        permissionState = permissionState,
    )

    BackHandler(enabled = state.isOpen) {
        state.close()
    }

    if (state.isOpen) {
        ConversationMediaPicker(
            modifier = modifier
                .fillMaxSize()
                .testTag(CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG),
            attachments = attachments,
            conversationTitle = conversationTitle,
            isSendActionEnabled = isSendActionEnabled,
            state = state,
            cameraPermissionGranted = permissionState.cameraPermissionGranted,
            audioPermissionGranted = permissionState.audioPermissionGranted,
            onClose = state::close,
            onAttachmentPreviewClick = onAttachmentPreviewClick,
            onAttachmentCaptionChange = onAttachmentCaptionChange,
            onAttachmentRemove = onAttachmentRemove,
            photoPickerSourceContentUriByAttachmentContentUri =
            photoPickerSourceContentUriByAttachmentContentUri,
            onPhotoPickerMediaSelected = onPhotoPickerMediaSelected,
            onPhotoPickerMediaDeselected = onPhotoPickerMediaDeselected,
            onRequestAudioPermission = {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onRequestCameraPermission = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onCapturedMediaReady = onCapturedMediaReady,
            onSendClick = onSendClick,
        )
    }
}
