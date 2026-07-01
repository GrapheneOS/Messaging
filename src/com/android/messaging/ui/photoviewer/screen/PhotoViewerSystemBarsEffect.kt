package com.android.messaging.ui.photoviewer.screen

import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.messaging.ui.core.findActivityWindow
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode

@Composable
internal fun PhotoViewerSystemBarsEffect(
    displayMode: PhotoViewerDisplayMode,
) {
    val view = LocalView.current
    val window = view.context.findActivityWindow()

    DisposableEffect(window, view) {
        val controller = createWindowInsetsController(
            window = window,
            view = view,
        )
        controller?.systemBarsBehavior = WindowInsetsControllerCompat
            .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val systemBars = WindowInsetsCompat.Type.systemBars()
        onDispose {
            controller?.show(systemBars)
        }
    }

    LaunchedEffect(window, view, displayMode) {
        val controller = createWindowInsetsController(
            window = window,
            view = view,
        )
        val systemBars = WindowInsetsCompat.Type.systemBars()
        when (displayMode) {
            PhotoViewerDisplayMode.Carousel -> controller?.show(systemBars)
            PhotoViewerDisplayMode.Immersive -> controller?.hide(systemBars)
        }
    }
}

private fun createWindowInsetsController(
    window: Window?,
    view: View,
): WindowInsetsControllerCompat? {
    return window?.let {
        WindowInsetsControllerCompat(
            it,
            view,
        )
    }
}
