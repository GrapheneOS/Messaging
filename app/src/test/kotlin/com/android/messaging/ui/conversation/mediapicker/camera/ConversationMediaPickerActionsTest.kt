package com.android.messaging.ui.conversation.mediapicker.camera

import androidx.camera.core.ImageCapture
import com.android.messaging.data.media.model.ConversationCapturedMedia
import com.android.messaging.util.ContentType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ConversationMediaPickerActionsTest {

    @Test
    fun photoFlashMode_cyclesThroughSupportedModes() {
        assertEquals(ConversationPhotoFlashMode.Auto, ConversationPhotoFlashMode.Off.next())
        assertEquals(ConversationPhotoFlashMode.On, ConversationPhotoFlashMode.Auto.next())
        assertEquals(ConversationPhotoFlashMode.Off, ConversationPhotoFlashMode.On.next())
    }

    @Test
    fun photoFlashMode_exposesImageCaptureFlashModes() {
        assertEquals(
            ImageCapture.FLASH_MODE_OFF,
            ConversationPhotoFlashMode.Off.imageCaptureFlashMode,
        )
        assertEquals(
            ImageCapture.FLASH_MODE_AUTO,
            ConversationPhotoFlashMode.Auto.imageCaptureFlashMode,
        )
        assertEquals(
            ImageCapture.FLASH_MODE_ON,
            ConversationPhotoFlashMode.On.imageCaptureFlashMode,
        )
    }

    @Test
    fun photoCaptureRequest_doesNotCaptureWhenAttachmentStartIsRejected() {
        val cameraController = mockk<ConversationCameraController>(relaxed = true)
        var readyCount = 0
        var reviewCount = 0

        handlePhotoCaptureRequest(
            cameraController = cameraController,
            onAttachmentStartRequest = { false },
            onCapturedMediaReady = { readyCount++ },
            onShowReview = { reviewCount++ },
        )

        verify(exactly = 0) {
            cameraController.capturePhoto(
                onCaptured = any(),
                onError = any(),
            )
        }
        assertEquals(0, readyCount)
        assertEquals(0, reviewCount)
    }

    @Test
    fun photoCaptureRequest_forwardsCapturedMediaAndShowsReview() {
        val cameraController = mockk<ConversationCameraController>()
        val capturedMedia = ConversationCapturedMedia(
            contentUri = CONTENT_URI,
            contentType = ContentType.IMAGE_JPEG,
        )
        var readyMedia: ConversationCapturedMedia? = null
        var reviewedContentUri: String? = null
        every {
            cameraController.capturePhoto(
                onCaptured = any(),
                onError = any(),
            )
        } answers {
            firstArg<(ConversationCapturedMedia) -> Unit>().invoke(capturedMedia)
        }

        handlePhotoCaptureRequest(
            cameraController = cameraController,
            onAttachmentStartRequest = { true },
            onCapturedMediaReady = { media -> readyMedia = media },
            onShowReview = { contentUri -> reviewedContentUri = contentUri },
        )

        assertEquals(capturedMedia, readyMedia)
        assertEquals(CONTENT_URI, reviewedContentUri)
    }

    @Test
    fun videoCaptureRequest_stopsActiveRecordingWithoutStartingAttachment() {
        val cameraController = mockk<ConversationCameraController>(relaxed = true)
        var attachmentStartCount = 0

        handleVideoCaptureRequest(
            cameraController = cameraController,
            isRecording = true,
            onAttachmentStartRequest = {
                attachmentStartCount++
                true
            },
            onCapturedMediaReady = {},
            onShowReview = {},
        )

        verify(exactly = 1) {
            cameraController.stopVideoRecording()
        }
        verify(exactly = 0) {
            cameraController.startVideoRecording(
                withAudio = any(),
                onCaptured = any(),
                onDiscarded = any(),
                onError = any(),
            )
        }
        assertEquals(0, attachmentStartCount)
    }

    @Test
    fun videoCaptureRequest_startsRecordingAndForwardsCapturedMedia() {
        val cameraController = mockk<ConversationCameraController>()
        val capturedMedia = ConversationCapturedMedia(
            contentUri = CONTENT_URI,
            contentType = ContentType.VIDEO_MP4,
        )
        var readyMedia: ConversationCapturedMedia? = null
        var reviewedContentUri: String? = null
        every {
            cameraController.startVideoRecording(
                withAudio = true,
                onCaptured = any(),
                onDiscarded = any(),
                onError = any(),
            )
        } answers {
            secondArg<(ConversationCapturedMedia) -> Unit>().invoke(capturedMedia)
        }

        handleVideoCaptureRequest(
            cameraController = cameraController,
            isRecording = false,
            onAttachmentStartRequest = { true },
            onCapturedMediaReady = { media -> readyMedia = media },
            onShowReview = { contentUri -> reviewedContentUri = contentUri },
        )

        assertEquals(capturedMedia, readyMedia)
        assertEquals(CONTENT_URI, reviewedContentUri)
    }

    @Test
    fun cameraUtilityRequests_delegateToController() {
        val cameraController = mockk<ConversationCameraController>(relaxed = true)

        handleSwitchCameraRequest(cameraController = cameraController)
        handleToggleFlashRequest(cameraController = cameraController)

        verify(exactly = 1) {
            cameraController.switchCamera(onError = any())
        }
        verify(exactly = 1) {
            cameraController.cyclePhotoFlashMode(onError = any())
        }
    }

    private companion object {
        private const val CONTENT_URI = "content://media/captured/1"
    }
}
