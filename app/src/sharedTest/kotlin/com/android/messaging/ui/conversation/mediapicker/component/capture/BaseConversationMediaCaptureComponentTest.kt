package com.android.messaging.ui.conversation.mediapicker.component.capture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_MEDIA_CAPTURE_SHUTTER_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.mediapicker.ConversationCaptureMode
import com.android.messaging.ui.conversation.mediapicker.camera.ConversationPhotoFlashMode
import com.android.messaging.ui.core.AppTheme
import io.mockk.clearAllMocks
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule

internal abstract class BaseConversationMediaCaptureComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    protected val onCloseClick = mockk<() -> Unit>(relaxed = true)
    protected val onFlashClick = mockk<() -> Unit>(relaxed = true)
    protected val onCaptureClick = mockk<() -> Unit>(relaxed = true)
    protected val onPhotoModeClick = mockk<() -> Unit>(relaxed = true)
    protected val onSwitchCameraClick = mockk<() -> Unit>(relaxed = true)
    protected val onVideoModeClick = mockk<() -> Unit>(relaxed = true)
    protected val onRequestAudioPermission = mockk<() -> Unit>(relaxed = true)
    protected val onRequestCameraPermission = mockk<() -> Unit>(relaxed = true)
    protected val onPhotoCaptureClick = mockk<() -> Unit>(relaxed = true)
    protected val onToggleFlashClick = mockk<() -> Unit>(relaxed = true)
    protected val onVideoCaptureClick = mockk<() -> Unit>(relaxed = true)

    @Before
    fun setUpBaseConversationMediaCaptureComponentTest() {
        clearAllMocks()
    }

    protected fun setTopBarContent(
        captureMode: ConversationCaptureMode = ConversationCaptureMode.Photo,
        hasFlashUnit: Boolean = true,
        isPhotoCaptureInProgress: Boolean = false,
        isRecording: Boolean = false,
        photoFlashMode: ConversationPhotoFlashMode = ConversationPhotoFlashMode.Off,
    ) {
        setThemedContent {
            ConversationMediaCaptureTopBar(
                captureMode = captureMode,
                hasFlashUnit = hasFlashUnit,
                isPhotoCaptureInProgress = isPhotoCaptureInProgress,
                isRecording = isRecording,
                photoFlashMode = photoFlashMode,
                onCloseClick = onCloseClick,
                onFlashClick = onFlashClick,
            )
        }
    }

    protected fun setControlsContent(
        captureMode: ConversationCaptureMode = ConversationCaptureMode.Photo,
        isPhotoCaptureInProgress: Boolean = false,
        isRecording: Boolean = false,
        recordingDurationMillis: Long = 0L,
    ) {
        setThemedContent {
            ConversationMediaCaptureControls(
                captureMode = captureMode,
                isPhotoCaptureInProgress = isPhotoCaptureInProgress,
                isRecording = isRecording,
                recordingDurationMillis = recordingDurationMillis,
                onCaptureClick = onCaptureClick,
                onPhotoModeClick = onPhotoModeClick,
                onSwitchCameraClick = onSwitchCameraClick,
                onVideoModeClick = onVideoModeClick,
            )
        }
    }

    protected fun setShutterButtonContent(
        captureMode: ConversationCaptureMode,
        isPhotoCaptureInProgress: Boolean = false,
        isRecording: Boolean = false,
    ) {
        setThemedContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ConversationMediaCaptureShutterButton(
                    modifier = Modifier
                        .testTag(CONVERSATION_MEDIA_CAPTURE_SHUTTER_BUTTON_TEST_TAG),
                    captureMode = captureMode,
                    isPhotoCaptureInProgress = isPhotoCaptureInProgress,
                    isRecording = isRecording,
                    onClick = onCaptureClick,
                )
            }
        }
    }

    protected fun setCaptureContent(
        audioPermissionGranted: Boolean = true,
        captureMode: ConversationCaptureMode = ConversationCaptureMode.Photo,
        cameraPermissionGranted: Boolean = true,
        hasFlashUnit: Boolean = true,
        isPhotoCaptureInProgress: Boolean = false,
        isRecording: Boolean = false,
        photoFlashMode: ConversationPhotoFlashMode = ConversationPhotoFlashMode.Off,
        recordingDurationMillis: Long = 0L,
    ) {
        setThemedContent {
            ConversationMediaCaptureContent(
                modifier = Modifier.fillMaxSize(),
                audioPermissionGranted = audioPermissionGranted,
                captureMode = captureMode,
                cameraPermissionGranted = cameraPermissionGranted,
                hasFlashUnit = hasFlashUnit,
                isPhotoCaptureInProgress = isPhotoCaptureInProgress,
                isRecording = isRecording,
                photoFlashMode = photoFlashMode,
                onCloseClick = onCloseClick,
                onRequestAudioPermission = onRequestAudioPermission,
                onPhotoCaptureClick = onPhotoCaptureClick,
                onPhotoModeClick = onPhotoModeClick,
                onSwitchCameraClick = onSwitchCameraClick,
                onToggleFlashClick = onToggleFlashClick,
                onVideoCaptureClick = onVideoCaptureClick,
                onVideoModeClick = onVideoModeClick,
                recordingDurationMillis = recordingDurationMillis,
            )
        }
    }

    protected fun setPreviewSurfaceContent(
        cameraPermissionGranted: Boolean,
    ) {
        setThemedContent {
            ConversationMediaCameraPreviewSurface(
                modifier = Modifier.fillMaxSize(),
                cameraPermissionGranted = cameraPermissionGranted,
                contentPadding = PaddingValues(),
                surfaceRequest = null,
                onRequestCameraPermission = onRequestCameraPermission,
            )
        }
    }

    protected fun clickCaptureShutterButton() {
        composeTestRule
            .onNodeWithTag(CONVERSATION_MEDIA_CAPTURE_SHUTTER_BUTTON_TEST_TAG)
            .performClick()
    }

    protected fun string(resourceId: Int): String {
        return targetContext.getString(resourceId)
    }

    protected fun closeDescription(): String {
        return string(R.string.conversation_media_picker_close_content_description)
    }

    protected fun flashDescription(): String {
        return string(R.string.conversation_media_picker_cycle_flash_mode_content_description)
    }

    protected fun switchCameraDescription(): String {
        return string(R.string.camera_switch_camera_facing)
    }

    protected fun photoModeLabel(): String {
        return string(R.string.conversation_media_picker_photo_mode)
    }

    protected fun videoModeLabel(): String {
        return string(R.string.conversation_media_picker_video_mode)
    }

    private fun setThemedContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            AppTheme(content = content)
        }
    }
}
