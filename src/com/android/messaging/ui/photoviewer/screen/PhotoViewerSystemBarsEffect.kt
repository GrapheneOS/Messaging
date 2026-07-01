package com.android.messaging.ui.photoviewer.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

    DisposableEffect(window, view, displayMode) {
        val controller = window?.let {
            WindowInsetsControllerCompat(
                it,
                view,
            )
        }

        controller?.systemBarsBehavior = WindowInsetsControllerCompat
            .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val systemBars = WindowInsetsCompat.Type.systemBars()
        when (displayMode) {
            PhotoViewerDisplayMode.Carousel -> controller?.show(systemBars)
            PhotoViewerDisplayMode.Immersive -> controller?.hide(systemBars)
        }

        onDispose {
            controller?.show(systemBars)
        }
    }
}
