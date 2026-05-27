package com.android.messaging.ui.conversation.mediapicker.component.capture

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.mediapicker.ConversationCaptureMode
import com.android.messaging.ui.conversation.mediapicker.camera.ConversationPhotoFlashMode
import com.android.messaging.ui.conversation.mediapicker.component.PermissionFallback
import com.android.messaging.ui.core.MessagingPreviewTheme

@Composable
internal fun ConversationMediaCameraPreviewSurface(
    modifier: Modifier = Modifier,
    cameraPermissionGranted: Boolean,
    contentPadding: PaddingValues,
    surfaceRequest: SurfaceRequest?,
    onRequestCameraPermission: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.scrim),
    ) {
        when {
            !cameraPermissionGranted -> {
                ConversationMediaCameraPermissionFallback(
                    contentPadding = contentPadding,
                    onRequestCameraPermission = onRequestCameraPermission,
                )
            }

            surfaceRequest == null -> {
                ConversationMediaCameraLoadingState()
            }

            else -> {
                ConversationMediaCameraViewfinder(
                    surfaceRequest = surfaceRequest,
                )
            }
        }
    }
}

@Composable
private fun ConversationMediaCameraPermissionFallback(
    contentPadding: PaddingValues,
    onRequestCameraPermission: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = contentPadding)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        PermissionFallback(
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = null,
                )
            },
            message = stringResource(
                id = R.string.conversation_media_picker_camera_permission_message,
            ),
            actionLabel = stringResource(
                id = R.string.conversation_media_picker_allow_camera,
            ),
            onActionClick = onRequestCameraPermission,
        )
    }
}

@Composable
private fun ConversationMediaCameraLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ConversationMediaCameraViewfinder(
    surfaceRequest: SurfaceRequest,
) {
    CameraXViewfinder(
        modifier = Modifier
            .fillMaxSize(),
        surfaceRequest = surfaceRequest,
    )
}

@Composable
internal fun ConversationMediaCaptureContent(
    modifier: Modifier = Modifier,
    audioPermissionGranted: Boolean,
    captureMode: ConversationCaptureMode,
    cameraPermissionGranted: Boolean,
    hasFlashUnit: Boolean,
    isPhotoCaptureInProgress: Boolean,
    isRecording: Boolean,
    photoFlashMode: ConversationPhotoFlashMode,
    onCloseClick: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onPhotoCaptureClick: () -> Unit,
    onPhotoModeClick: () -> Unit,
    onSwitchCameraClick: () -> Unit,
    onToggleFlashClick: () -> Unit,
    onVideoCaptureClick: () -> Unit,
    onVideoModeClick: () -> Unit,
    recordingDurationMillis: Long,
) {
    Box(
        modifier = modifier,
    ) {
        ConversationMediaCaptureTopBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            captureMode = captureMode,
            hasFlashUnit = cameraPermissionGranted && hasFlashUnit,
            isPhotoCaptureInProgress = isPhotoCaptureInProgress,
            isRecording = isRecording,
            photoFlashMode = photoFlashMode,
            onCloseClick = onCloseClick,
            onFlashClick = onToggleFlashClick,
        )

        if (cameraPermissionGranted) {
            ConversationMediaCaptureControls(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                captureMode = captureMode,
                isPhotoCaptureInProgress = isPhotoCaptureInProgress,
                isRecording = isRecording,
                recordingDurationMillis = recordingDurationMillis,
                onCaptureClick = {
                    when (captureMode) {
                        ConversationCaptureMode.Video -> {
                            when {
                                !isRecording && !audioPermissionGranted -> {
                                    onRequestAudioPermission()
                                }

                                else -> onVideoCaptureClick()
                            }
                        }

                        else -> onPhotoCaptureClick()
                    }
                },
                onPhotoModeClick = onPhotoModeClick,
                onSwitchCameraClick = onSwitchCameraClick,
                onVideoModeClick = onVideoModeClick,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationMediaCameraPreviewSurfacePermissionPreview() {
    MessagingPreviewTheme {
        ConversationMediaCameraPreviewSurface(
            modifier = Modifier.fillMaxSize(),
            cameraPermissionGranted = false,
            contentPadding = PaddingValues(),
            surfaceRequest = null,
            onRequestCameraPermission = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMediaCaptureContentPreview() {
    MessagingPreviewTheme {
        ConversationMediaCaptureContent(
            modifier = Modifier.fillMaxSize(),
            audioPermissionGranted = true,
            captureMode = ConversationCaptureMode.Video,
            cameraPermissionGranted = true,
            hasFlashUnit = true,
            isPhotoCaptureInProgress = false,
            isRecording = true,
            photoFlashMode = ConversationPhotoFlashMode.Off,
            onCloseClick = {},
            onRequestAudioPermission = {},
            onPhotoCaptureClick = {},
            onPhotoModeClick = {},
            onSwitchCameraClick = {},
            onToggleFlashClick = {},
            onVideoCaptureClick = {},
            onVideoModeClick = {},
            recordingDurationMillis = 37_000L,
        )
    }
}
