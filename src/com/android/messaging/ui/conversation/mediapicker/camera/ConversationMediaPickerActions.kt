package com.android.messaging.ui.conversation.mediapicker.camera

import com.android.messaging.R
import com.android.messaging.data.media.model.ConversationCapturedMedia
import com.android.messaging.util.UiUtils

internal fun handlePhotoCaptureRequest(
    cameraController: ConversationCameraController,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onShowReview: (String) -> Unit,
) {
    cameraController.capturePhoto(
        onCaptured = { capturedMedia ->
            onCapturedMediaReady(capturedMedia)
            onShowReview(capturedMedia.contentUri)
        },
        onError = {
            UiUtils.showToastAtBottom(
                R.string.camera_error_failure_taking_picture,
            )
        },
    )
}

internal fun handleSwitchCameraRequest(cameraController: ConversationCameraController) {
    cameraController.switchCamera(
        onError = {
            UiUtils.showToastAtBottom(
                R.string.camera_error_opening,
            )
        },
    )
}

internal fun handleToggleFlashRequest(cameraController: ConversationCameraController) {
    cameraController.cyclePhotoFlashMode(
        onError = {
            UiUtils.showToastAtBottom(
                R.string.camera_error_opening,
            )
        },
    )
}

internal fun handleVideoCaptureRequest(
    cameraController: ConversationCameraController,
    isRecording: Boolean,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onShowReview: (String) -> Unit,
) {
    if (isRecording) {
        cameraController.stopVideoRecording()
        return
    }

    cameraController.startVideoRecording(
        withAudio = true,
        onCaptured = { capturedMedia ->
            onCapturedMediaReady(capturedMedia)
            onShowReview(capturedMedia.contentUri)
        },
        onDiscarded = {},
        onError = {
            UiUtils.showToastAtBottom(
                R.string.camera_media_failure,
            )
        },
    )
}
