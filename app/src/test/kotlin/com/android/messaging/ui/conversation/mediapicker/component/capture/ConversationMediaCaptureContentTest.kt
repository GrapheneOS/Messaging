package com.android.messaging.ui.conversation.mediapicker.component.capture

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_MEDIA_CAPTURE_SHUTTER_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.mediapicker.ConversationCaptureMode
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaCaptureContentTest :
    BaseConversationMediaCaptureComponentTest() {

    @Test
    fun previewSurface_cameraDeniedRendersPermissionFallbackAndForwardsAction() {
        setPreviewSurfaceContent(cameraPermissionGranted = false)

        composeTestRule
            .onNodeWithText(string(R.string.conversation_media_picker_camera_permission_message))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.conversation_media_picker_allow_camera))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onRequestCameraPermission.invoke()
            }
        }
    }

    @Test
    fun previewSurface_cameraGrantedWithoutSurfaceRendersLoadingState() {
        setPreviewSurfaceContent(cameraPermissionGranted = true)

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.conversation_media_picker_camera_permission_message))
            .assertDoesNotExist()
    }

    @Test
    fun cameraPermissionDenied_hidesCaptureControlsAndFlash() {
        setCaptureContent(
            cameraPermissionGranted = false,
            hasFlashUnit = true,
        )

        composeTestRule
            .onNodeWithContentDescription(closeDescription())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(flashDescription())
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription(switchCameraDescription())
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(photoModeLabel())
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(videoModeLabel())
            .assertDoesNotExist()
    }

    @Test
    fun photoCaptureClick_forwardsPhotoCaptureOnly() {
        setCaptureContent(
            captureMode = ConversationCaptureMode.Photo,
        )

        clickCaptureControl()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onPhotoCaptureClick.invoke()
            }
            verify(exactly = 0) {
                onVideoCaptureClick.invoke()
            }
            verify(exactly = 0) {
                onRequestAudioPermission.invoke()
            }
        }
    }

    @Test
    fun videoCaptureWithoutAudioPermission_requestsAudioOnly() {
        setCaptureContent(
            audioPermissionGranted = false,
            captureMode = ConversationCaptureMode.Video,
        )

        clickCaptureControl()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onRequestAudioPermission.invoke()
            }
            verify(exactly = 0) {
                onVideoCaptureClick.invoke()
            }
            verify(exactly = 0) {
                onPhotoCaptureClick.invoke()
            }
        }
    }

    @Test
    fun videoCaptureWithAudioPermission_forwardsVideoCapture() {
        setCaptureContent(
            audioPermissionGranted = true,
            captureMode = ConversationCaptureMode.Video,
        )

        clickCaptureControl()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onVideoCaptureClick.invoke()
            }
            verify(exactly = 0) {
                onRequestAudioPermission.invoke()
            }
            verify(exactly = 0) {
                onPhotoCaptureClick.invoke()
            }
        }
    }

    private fun clickCaptureControl() {
        composeTestRule
            .onNodeWithTag(CONVERSATION_MEDIA_CAPTURE_SHUTTER_BUTTON_TEST_TAG)
            .performClick()
    }
}
