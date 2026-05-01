package com.android.messaging.ui.conversation.mediapicker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.data.media.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.mediapicker.camera.ConversationCameraController
import com.android.messaging.ui.conversation.mediapicker.component.capture.ConversationMediaCameraPreviewSurface

@Composable
internal fun ConversationMediaPickerCaptureScene(
    modifier: Modifier = Modifier,
    cameraController: ConversationCameraController,
    contentPadding: PaddingValues,
    captureMode: ConversationCaptureMode,
    cameraPermissionGranted: Boolean,
    audioPermissionGranted: Boolean,
    onClose: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onShowReview: (String) -> Unit,
    onCaptureModeChange: (ConversationCaptureMode) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        ConversationMediaCameraPreviewRoute(
            modifier = Modifier
                .fillMaxSize(),
            cameraController = cameraController,
            cameraPermissionGranted = cameraPermissionGranted,
            onRequestCameraPermission = onRequestCameraPermission,
        )

        ConversationMediaCaptureRoute(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = contentPadding),
            cameraController = cameraController,
            audioPermissionGranted = audioPermissionGranted,
            captureMode = captureMode,
            onClose = onClose,
            onRequestAudioPermission = onRequestAudioPermission,
            onShowReview = onShowReview,
            onCapturedMediaReady = onCapturedMediaReady,
            onCaptureModeChange = onCaptureModeChange,
        )
    }
}

@Composable
private fun ConversationMediaCameraPreviewRoute(
    modifier: Modifier = Modifier,
    cameraController: ConversationCameraController,
    cameraPermissionGranted: Boolean,
    onRequestCameraPermission: () -> Unit,
) {
    val surfaceRequest = cameraController.surfaceRequest.collectAsStateWithLifecycle()

    ConversationMediaCameraPreviewSurface(
        modifier = modifier,
        cameraPermissionGranted = cameraPermissionGranted,
        surfaceRequest = surfaceRequest.value,
        onRequestCameraPermission = onRequestCameraPermission,
    )
}
