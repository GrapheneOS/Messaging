package com.android.messaging.ui.conversation.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.data.media.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.mediapicker.camera.ConversationCameraController
import com.android.messaging.ui.conversation.mediapicker.camera.handlePhotoCaptureRequest
import com.android.messaging.ui.conversation.mediapicker.camera.handleSwitchCameraRequest
import com.android.messaging.ui.conversation.mediapicker.camera.handleToggleFlashRequest
import com.android.messaging.ui.conversation.mediapicker.camera.handleVideoCaptureRequest
import com.android.messaging.ui.conversation.mediapicker.component.capture.ConversationMediaCaptureContent

@Composable
internal fun ConversationMediaCaptureRoute(
    modifier: Modifier = Modifier,
    cameraController: ConversationCameraController,
    audioPermissionGranted: Boolean,
    captureMode: ConversationCaptureMode,
    onClose: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onShowReview: (String) -> Unit,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onCaptureModeChange: (ConversationCaptureMode) -> Unit,
) {
    val hasFlashUnit = cameraController.hasFlashUnit.collectAsStateWithLifecycle()
    val isPhotoCaptureInProgress = cameraController.isPhotoCaptureInProgress
        .collectAsStateWithLifecycle()

    val isRecording = cameraController.isRecording.collectAsStateWithLifecycle()
    val photoFlashMode = cameraController.photoFlashMode.collectAsStateWithLifecycle()
    val recordingDurationMillis = cameraController.recordingDurationMillis
        .collectAsStateWithLifecycle()

    ConversationMediaCaptureContent(
        modifier = modifier,
        audioPermissionGranted = audioPermissionGranted,
        captureMode = captureMode,
        hasFlashUnit = hasFlashUnit.value,
        isPhotoCaptureInProgress = isPhotoCaptureInProgress.value,
        isRecording = isRecording.value,
        photoFlashMode = photoFlashMode.value,
        onCloseClick = {
            if (isRecording.value) {
                cameraController.cancelVideoRecording()
            }
            onClose()
        },
        onRequestAudioPermission = onRequestAudioPermission,
        onPhotoCaptureClick = {
            handlePhotoCaptureRequest(
                cameraController = cameraController,
                onCapturedMediaReady = onCapturedMediaReady,
                onShowReview = onShowReview,
            )
        },
        onPhotoModeClick = {
            onCaptureModeChange(ConversationCaptureMode.Photo)
        },
        onSwitchCameraClick = {
            handleSwitchCameraRequest(
                cameraController = cameraController,
            )
        },
        onToggleFlashClick = {
            handleToggleFlashRequest(
                cameraController = cameraController,
            )
        },
        onVideoCaptureClick = {
            handleVideoCaptureRequest(
                cameraController = cameraController,
                isRecording = isRecording.value,
                onCapturedMediaReady = onCapturedMediaReady,
                onShowReview = onShowReview,
            )
        },
        onVideoModeClick = {
            onCaptureModeChange(ConversationCaptureMode.Video)
        },
        recordingDurationMillis = recordingDurationMillis.value,
    )
}
