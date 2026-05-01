package com.android.messaging.ui.conversation.mediapicker.model

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

@Stable
internal class ConversationMediaPickerPermissionState(
    context: Context,
) {
    var audioPermissionGranted by mutableStateOf(value = hasAudioPermission(context = context))
    var cameraPermissionGranted by mutableStateOf(value = hasCameraPermission(context = context))

    fun refresh(context: Context) {
        audioPermissionGranted = hasAudioPermission(context = context)
        cameraPermissionGranted = hasCameraPermission(context = context)
    }
}

private fun hasCameraPermission(context: Context): Boolean {
    return isPermissionGranted(
        context = context,
        permission = Manifest.permission.CAMERA,
    )
}

private fun hasAudioPermission(context: Context): Boolean {
    return isPermissionGranted(
        context = context,
        permission = Manifest.permission.RECORD_AUDIO,
    )
}

private fun isPermissionGranted(
    context: Context,
    permission: String,
): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission,
    ) == PackageManager.PERMISSION_GRANTED
}
