package com.android.messaging.ui.conversation.mediapicker

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.data.media.model.ConversationCapturedMedia
import io.mockk.every
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerCaptureRouteTest : BaseConversationMediaPickerTest() {

    @Test
    fun closeWhileIdle_closesWithoutCancellingRecording() {
        setCaptureRouteContent()

        closeButton().performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onClose.invoke()
            }
            verify(exactly = 0) {
                cameraController.cancelVideoRecording()
            }
        }
    }

    @Test
    fun closeWhileRecording_cancelsRecordingBeforeClosing() {
        cameraState.isRecording.value = true

        setCaptureRouteContent(
            captureMode = ConversationCaptureMode.Video,
        )

        closeButton().performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                cameraController.cancelVideoRecording()
            }
            verify(exactly = 1) {
                onClose.invoke()
            }
        }
    }

    @Test
    fun photoCapture_whenAttachmentStartRejectedDoesNotCapture() {
        every { onAttachmentStartRequest.invoke() } returns false

        setCaptureRouteContent(
            captureMode = ConversationCaptureMode.Photo,
        )

        clickCaptureControl()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentStartRequest.invoke()
            }
            verify(exactly = 0) {
                cameraController.capturePhoto(
                    onCaptured = any(),
                    onError = any(),
                )
            }
        }
    }

    @Test
    fun photoCapture_successPublishesCapturedMediaAndShowsReview() {
        val capturedPhoto = capturedPhoto()
        every {
            cameraController.capturePhoto(
                onCaptured = any(),
                onError = any(),
            )
        } answers {
            arg<(ConversationCapturedMedia) -> Unit>(0).invoke(capturedPhoto)
        }

        setCaptureRouteContent(
            captureMode = ConversationCaptureMode.Photo,
        )

        clickCaptureControl()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                cameraController.capturePhoto(
                    onCaptured = any(),
                    onError = any(),
                )
            }
            verify(exactly = 1) {
                onCapturedMediaReady.invoke(capturedPhoto)
            }
            verify(exactly = 1) {
                onShowReview.invoke(CAPTURED_PHOTO_URI)
            }
        }
    }

    @Test
    fun videoCaptureWhileRecording_stopsRecordingWithoutStartingAttachment() {
        cameraState.isRecording.value = true

        setCaptureRouteContent(
            captureMode = ConversationCaptureMode.Video,
        )

        clickCaptureControl()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                cameraController.stopVideoRecording()
            }
            verify(exactly = 0) {
                onAttachmentStartRequest.invoke()
            }
            verify(exactly = 0) {
                cameraController.startVideoRecording(
                    withAudio = any(),
                    onCaptured = any(),
                    onDiscarded = any(),
                    onError = any(),
                )
            }
        }
    }

    @Test
    fun videoCapture_whenAttachmentStartRejectedDoesNotStartRecording() {
        every { onAttachmentStartRequest.invoke() } returns false

        setCaptureRouteContent(
            captureMode = ConversationCaptureMode.Video,
        )

        clickCaptureControl()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onAttachmentStartRequest.invoke()
            }
            verify(exactly = 0) {
                cameraController.startVideoRecording(
                    withAudio = any(),
                    onCaptured = any(),
                    onDiscarded = any(),
                    onError = any(),
                )
            }
        }
    }

    @Test
    fun videoCapture_successPublishesCapturedMediaAndShowsReview() {
        val capturedVideo = capturedVideo()
        every {
            cameraController.startVideoRecording(
                withAudio = true,
                onCaptured = any(),
                onDiscarded = any(),
                onError = any(),
            )
        } answers {
            arg<(ConversationCapturedMedia) -> Unit>(1).invoke(capturedVideo)
        }

        setCaptureRouteContent(
            captureMode = ConversationCaptureMode.Video,
        )

        clickCaptureControl()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                cameraController.startVideoRecording(
                    withAudio = true,
                    onCaptured = any(),
                    onDiscarded = any(),
                    onError = any(),
                )
            }
            verify(exactly = 1) {
                onCapturedMediaReady.invoke(capturedVideo)
            }
            verify(exactly = 1) {
                onShowReview.invoke(CAPTURED_VIDEO_URI)
            }
        }
    }

    @Test
    fun switchCameraAndFlash_delegateToCameraController() {
        cameraState.hasFlashUnit.value = true

        setCaptureRouteContent()

        composeTestRule
            .onNodeWithContentDescription(
                targetContext.getString(R.string.camera_switch_camera_facing)
            )
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(
                targetContext.getString(
                    R.string.conversation_media_picker_cycle_flash_mode_content_description,
                ),
            )
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                cameraController.switchCamera(onError = any())
            }
            verify(exactly = 1) {
                cameraController.cyclePhotoFlashMode(onError = any())
            }
        }
    }

    private fun closeButton(): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithContentDescription(
            targetContext.getString(R.string.conversation_media_picker_close_content_description),
        )
    }
}
