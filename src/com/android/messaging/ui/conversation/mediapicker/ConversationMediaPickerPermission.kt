package com.android.messaging.ui.conversation.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.android.messaging.ui.conversation.mediapicker.model.ConversationMediaPickerPermissionState

@Composable
internal fun rememberConversationMediaPickerPermissionState():
    ConversationMediaPickerPermissionState {
    val context = LocalContext.current

    return remember(context) {
        ConversationMediaPickerPermissionState(
            context = context,
        )
    }
}

@Composable
internal fun RefreshConversationMediaPickerPermissionsEffect(
    permissionState: ConversationMediaPickerPermissionState,
) {
    val context = LocalContext.current

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
