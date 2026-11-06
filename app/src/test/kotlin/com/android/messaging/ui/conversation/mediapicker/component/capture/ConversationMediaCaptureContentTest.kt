package com.android.messaging.ui.conversation.mediapicker.component.capture

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

    @Test
    fun topBar_windowTallerThanPreview_keepsPaddingFromTopOfPreview() {
        setCaptureContent(
            modifier = Modifier.size(width = 300.dp, height = 460.dp),
            aspectRatio = PHOTO_ASPECT_RATIO,
        )

        assertTopBarPosition(
            top = 42.dp,
            closeLeft = 16.dp,
            flashLeft = 236.dp,
        )
    }

    @Test
    fun topBar_windowShorterThanPreview_keepsPaddingFromSidesOfPreview() {
        setCaptureContent(
            modifier = Modifier.size(width = 300.dp, height = 300.dp),
            aspectRatio = PHOTO_ASPECT_RATIO,
        )

        assertTopBarPosition(
            top = 12.dp,
            closeLeft = 53.5.dp,
            flashLeft = 198.5.dp,
        )
    }

    @Test
    fun topBar_aspectRatioUnknown_keepsPaddingFromWholeArea() {
        setCaptureContent(
            modifier = Modifier.size(width = 300.dp, height = 460.dp),
            aspectRatio = null,
        )

        assertTopBarPosition(
            top = 12.dp,
            closeLeft = 16.dp,
            flashLeft = 236.dp,
        )
    }

    private fun assertTopBarPosition(
        top: Dp,
        closeLeft: Dp,
        flashLeft: Dp,
    ) {
        composeTestRule
            .onNodeWithContentDescription(closeDescription())
            .assertTopPositionInRootIsEqualTo(expectedTop = top)
            .assertLeftPositionInRootIsEqualTo(expectedLeft = closeLeft)
        composeTestRule
            .onNodeWithContentDescription(flashDescription())
            .assertTopPositionInRootIsEqualTo(expectedTop = top)
            .assertLeftPositionInRootIsEqualTo(expectedLeft = flashLeft)
    }

    private fun clickCaptureControl() {
        composeTestRule
            .onNodeWithTag(CONVERSATION_MEDIA_CAPTURE_SHUTTER_BUTTON_TEST_TAG)
            .performClick()
    }

    private companion object {
        const val PHOTO_ASPECT_RATIO = 3f / 4f
    }
}
