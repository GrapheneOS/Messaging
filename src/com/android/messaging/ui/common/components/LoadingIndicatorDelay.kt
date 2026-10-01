package com.android.messaging.ui.common.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

// Matches ContentLoadingProgressBar, so loads faster than this never flash a loading UI.
internal val LOADING_INDICATOR_DELAY = 500.milliseconds

@Composable
internal fun rememberIsLoadingIndicatorVisible(isLoading: Boolean): Boolean {
    var isVisible by remember(isLoading) { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(LOADING_INDICATOR_DELAY)
            isVisible = true
        }
    }

    return isVisible
}
