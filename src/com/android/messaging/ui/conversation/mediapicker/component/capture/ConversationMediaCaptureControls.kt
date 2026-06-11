package com.android.messaging.ui.conversation.mediapicker.component.capture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_MEDIA_CAPTURE_SHUTTER_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.audio.formatConversationAudioDuration
import com.android.messaging.ui.conversation.mediapicker.ConversationCaptureMode
import com.android.messaging.ui.conversation.mediapicker.camera.ConversationPhotoFlashMode
import com.android.messaging.ui.conversation.mediapicker.component.PickerOverlayIconButton
import com.android.messaging.ui.conversation.mediapicker.component.pickerOverlayContainerColor
import com.android.messaging.ui.conversation.mediapicker.component.pickerOverlayContentColor
import com.android.messaging.ui.core.MessagingPreviewColumn

@Composable
internal fun ConversationMediaCaptureTopBar(
    modifier: Modifier = Modifier,
    captureMode: ConversationCaptureMode,
    hasFlashUnit: Boolean,
    isPhotoCaptureInProgress: Boolean,
    isRecording: Boolean,
    photoFlashMode: ConversationPhotoFlashMode,
    onCloseClick: () -> Unit,
    onFlashClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PickerOverlayIconButton(
            contentDescription = stringResource(
                id = R.string.conversation_media_picker_close_content_description,
            ),
            imageVector = Icons.Rounded.Close,
            onClick = onCloseClick,
        )
        if (hasFlashUnit && captureMode == ConversationCaptureMode.Photo) {
            PickerOverlayIconButton(
                contentDescription = stringResource(
                    id = R.string.conversation_media_picker_cycle_flash_mode_content_description,
                ),
                enabled = !isPhotoCaptureInProgress && !isRecording,
                imageVector = when (photoFlashMode) {
                    ConversationPhotoFlashMode.Auto -> Icons.Rounded.FlashAuto
                    ConversationPhotoFlashMode.Off -> Icons.Rounded.FlashOff
                    ConversationPhotoFlashMode.On -> Icons.Rounded.FlashOn
                },
                onClick = onFlashClick,
            )
        }
    }
}

@Composable
internal fun ConversationMediaCaptureControls(
    modifier: Modifier = Modifier,
    captureMode: ConversationCaptureMode,
    isPhotoCaptureInProgress: Boolean,
    isRecording: Boolean,
    recordingDurationMillis: Long,
    onCaptureClick: () -> Unit,
    onPhotoModeClick: () -> Unit,
    onSwitchCameraClick: () -> Unit,
    onVideoModeClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(48.dp))

            Column(
                modifier = Modifier
                    .weight(weight = 1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isRecording) {
                    ConversationMediaRecordingTimerPill(
                        durationMillis = recordingDurationMillis,
                    )
                }

                ConversationMediaCaptureShutterButton(
                    modifier = Modifier
                        .testTag(CONVERSATION_MEDIA_CAPTURE_SHUTTER_BUTTON_TEST_TAG),
                    captureMode = captureMode,
                    isPhotoCaptureInProgress = isPhotoCaptureInProgress,
                    isRecording = isRecording,
                    onClick = onCaptureClick,
                )

                ConversationMediaCaptureModeToggle(
                    captureMode = captureMode,
                    enabled = !isPhotoCaptureInProgress && !isRecording,
                    onPhotoModeClick = onPhotoModeClick,
                    onVideoModeClick = onVideoModeClick,
                )
            }
            PickerOverlayIconButton(
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                contentDescription = stringResource(
                    id = R.string.camera_switch_camera_facing,
                ),
                enabled = !isPhotoCaptureInProgress && !isRecording,
                imageVector = Icons.Rounded.Cameraswitch,
                onClick = onSwitchCameraClick,
            )
        }
    }
}

@Composable
private fun ConversationMediaCaptureModeToggle(
    captureMode: ConversationCaptureMode,
    enabled: Boolean,
    onPhotoModeClick: () -> Unit,
    onVideoModeClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = pickerOverlayContainerColor(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConversationMediaCaptureModeChip(
                isSelected = captureMode == ConversationCaptureMode.Photo,
                label = stringResource(id = R.string.conversation_media_picker_photo_mode),
                enabled = enabled,
                onClick = onPhotoModeClick,
            )

            ConversationMediaCaptureModeChip(
                isSelected = captureMode == ConversationCaptureMode.Video,
                label = stringResource(id = R.string.conversation_media_picker_video_mode),
                enabled = enabled,
                onClick = onVideoModeClick,
            )
        }
    }
}

@Composable
private fun ConversationMediaCaptureModeChip(
    isSelected: Boolean,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = when {
            isSelected -> MaterialTheme.colorScheme.secondaryContainer
            else -> pickerOverlayContainerColor(alpha = 0f)
        },
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> pickerOverlayContentColor(alpha = 0.9f)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ConversationMediaRecordingTimerPill(
    durationMillis: Long,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp),
            text = formatConversationAudioDuration(durationMillis = durationMillis),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMediaCaptureControlsPreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 24.dp)) {
            ConversationMediaCaptureTopBar(
                captureMode = ConversationCaptureMode.Photo,
                hasFlashUnit = true,
                isPhotoCaptureInProgress = false,
                isRecording = false,
                photoFlashMode = ConversationPhotoFlashMode.Auto,
                onCloseClick = {},
                onFlashClick = {},
            )
            ConversationMediaCaptureControls(
                captureMode = ConversationCaptureMode.Photo,
                isPhotoCaptureInProgress = false,
                isRecording = false,
                recordingDurationMillis = 0L,
                onCaptureClick = {},
                onPhotoModeClick = {},
                onSwitchCameraClick = {},
                onVideoModeClick = {},
            )
            ConversationMediaCaptureControls(
                captureMode = ConversationCaptureMode.Video,
                isPhotoCaptureInProgress = false,
                isRecording = true,
                recordingDurationMillis = 24_000L,
                onCaptureClick = {},
                onPhotoModeClick = {},
                onSwitchCameraClick = {},
                onVideoModeClick = {},
            )
        }
    }
}
