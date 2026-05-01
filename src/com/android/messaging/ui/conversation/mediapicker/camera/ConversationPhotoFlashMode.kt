package com.android.messaging.ui.conversation.mediapicker.camera

import androidx.camera.core.ImageCapture

internal enum class ConversationPhotoFlashMode(
    val imageCaptureFlashMode: Int,
) {
    Off(
        imageCaptureFlashMode = ImageCapture.FLASH_MODE_OFF,
    ),
    Auto(
        imageCaptureFlashMode = ImageCapture.FLASH_MODE_AUTO,
    ),
    On(
        imageCaptureFlashMode = ImageCapture.FLASH_MODE_ON,
    ),
    ;

    fun next(): ConversationPhotoFlashMode {
        return when (this) {
            Off -> Auto
            Auto -> On
            On -> Off
        }
    }
}
