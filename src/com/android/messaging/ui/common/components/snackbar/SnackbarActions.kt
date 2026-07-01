package com.android.messaging.ui.common.components.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

internal suspend fun SnackbarHostState.showActionSnackbar(
    message: String,
    actionLabel: String,
    duration: SnackbarDuration = SnackbarDuration.Short,
): Boolean {
    val result = showSnackbar(
        message = message,
        actionLabel = actionLabel,
        duration = duration,
    )

    return result == SnackbarResult.ActionPerformed
}
