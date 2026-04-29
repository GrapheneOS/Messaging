package com.android.messaging.ui.conversation.v2.mediapicker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

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

@Composable
internal fun rememberConversationMediaPickerPermissionState(
    context: Context,
): ConversationMediaPickerPermissionState {
    return remember(context) {
        ConversationMediaPickerPermissionState(
            context = context,
        )
    }
}

@Composable
internal fun RefreshConversationMediaPickerPermissionsEffect(
    context: Context,
    permissionState: ConversationMediaPickerPermissionState,
) {
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        permissionState.refresh(context = context)
    }
}

@Composable
internal fun HandleConversationMediaPickerVisibilityEffect(
    state: ConversationMediaPickerState,
    isImeVisible: Boolean,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?,
    messageFieldFocusRequester: FocusRequester,
) {
    LaunchedEffect(state.isOpen) {
        if (state.isOpen) {
            state.shouldRestoreKeyboard = isImeVisible
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            return@LaunchedEffect
        }

        if (!state.shouldRestoreKeyboard) {
            return@LaunchedEffect
        }

        messageFieldFocusRequester.requestFocus()
        keyboardController?.show()
        state.shouldRestoreKeyboard = false
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
