package com.android.messaging.ui.conversation.mediapicker

import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.data.media.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.mediapicker.camera.ConversationCameraController
import com.android.messaging.ui.conversation.mediapicker.component.capture.ConversationMediaCameraPreviewSurface

private const val NOMINAL_PHOTO_ASPECT_RATIO = 3f / 4f
private const val NOMINAL_VIDEO_ASPECT_RATIO = 9f / 16f

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
    onAttachmentStartRequest: () -> Boolean,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onShowReview: (String) -> Unit,
    onCaptureModeChange: (ConversationCaptureMode) -> Unit,
) {
    val cameraAspectRatio = when (captureMode) {
        ConversationCaptureMode.Photo -> cameraController.photoAspectRatio
        ConversationCaptureMode.Video -> cameraController.videoAspectRatio
    }.collectAsStateWithLifecycle()
    val isDisplayLandscape = rememberIsDisplayLandscape()
    val aspectRatio = when {
        !cameraPermissionGranted -> null

        else -> {
            cameraAspectRatio.value
                ?: captureMode.nominalAspectRatio(isLandscape = isDisplayLandscape)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        ConversationMediaCameraPreviewRoute(
            modifier = Modifier
                .fillMaxSize(),
            cameraController = cameraController,
            aspectRatio = aspectRatio,
            cameraPermissionGranted = cameraPermissionGranted,
            contentPadding = contentPadding,
            onRequestCameraPermission = onRequestCameraPermission,
        )

        ConversationMediaCaptureRoute(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = contentPadding),
            cameraController = cameraController,
            aspectRatio = aspectRatio,
            audioPermissionGranted = audioPermissionGranted,
            captureMode = captureMode,
            cameraPermissionGranted = cameraPermissionGranted,
            onClose = onClose,
            onRequestAudioPermission = onRequestAudioPermission,
            onAttachmentStartRequest = onAttachmentStartRequest,
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
    aspectRatio: Float?,
    cameraPermissionGranted: Boolean,
    contentPadding: PaddingValues,
    onRequestCameraPermission: () -> Unit,
) {
    val surfaceRequest = cameraController.surfaceRequest.collectAsStateWithLifecycle()

    ConversationMediaCameraPreviewSurface(
        modifier = modifier,
        aspectRatio = aspectRatio,
        cameraPermissionGranted = cameraPermissionGranted,
        contentPadding = contentPadding,
        surfaceRequest = surfaceRequest.value,
        onRequestCameraPermission = onRequestCameraPermission,
    )
}

@Composable
private fun rememberIsDisplayLandscape(): Boolean {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    return remember(context, configuration) {
        val displayBounds = context.getSystemService(WindowManager::class.java)
            .maximumWindowMetrics
            .bounds

        displayBounds.width() > displayBounds.height()
    }
}

private fun ConversationCaptureMode.nominalAspectRatio(isLandscape: Boolean): Float {
    val portraitAspectRatio = when (this) {
        ConversationCaptureMode.Photo -> NOMINAL_PHOTO_ASPECT_RATIO
        ConversationCaptureMode.Video -> NOMINAL_VIDEO_ASPECT_RATIO
    }

    return when {
        isLandscape -> 1f / portraitAspectRatio
        else -> portraitAspectRatio
    }
}
