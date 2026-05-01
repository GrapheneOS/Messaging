package com.android.messaging.ui.conversation.mediapicker.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCaptureException

internal sealed class ConversationCameraControllerException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

internal class CameraNotBoundException :
    ConversationCameraControllerException(
        message = "Camera is not bound",
    )

internal class CameraLensUnavailableException(
    lensFacing: Int,
) : ConversationCameraControllerException(
    message = "Requested camera lens is not available: ${resolveLensFacingName(
        lensFacing = lensFacing
    )}",
)

internal class PhotoCaptureFailedException(
    cause: ImageCaptureException,
) : ConversationCameraControllerException(
    message = "Photo capture failed",
    cause = cause,
)

internal class PhotoCaptureAlreadyInProgressException :
    ConversationCameraControllerException(
        message = "Photo capture is already in progress",
    )

internal class PhotoCaptureStartFailedException(
    cause: Throwable,
) : ConversationCameraControllerException(
    message = "Photo capture could not be started",
    cause = cause,
)

internal class RecordingAlreadyInProgressException :
    ConversationCameraControllerException(
        message = "Video recording is already in progress",
    )

internal class ScratchFileCreationFailedException(
    mediaLabel: String,
) : ConversationCameraControllerException(
    message = "Unable to create $mediaLabel scratch file",
)

internal class FlashUnavailableException :
    ConversationCameraControllerException(
        message = "Flash is not available for the current camera",
    )

internal class VideoRecordingFailedException(
    errorCode: Int,
    errorName: String,
    cause: Throwable? = null,
) : ConversationCameraControllerException(
    message = "Video recording failed: $errorName ($errorCode)",
    cause = cause,
)

private fun resolveLensFacingName(lensFacing: Int): String {
    return when (lensFacing) {
        CameraSelector.LENS_FACING_BACK -> "back"
        CameraSelector.LENS_FACING_FRONT -> "front"
        else -> "unknown"
    }
}
