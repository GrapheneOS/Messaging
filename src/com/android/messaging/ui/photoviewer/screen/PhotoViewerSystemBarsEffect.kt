package com.android.messaging.ui.photoviewer.screen

import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
    val controller = remember(window, view) {
        createWindowInsetsController(
            window = window,
            view = view,
        )
    } ?: return

    ImmersiveSystemBarsEffect(controller = controller)

    SystemBarsVisibilityEffect(
        controller = controller,
        displayMode = displayMode,
    )
}

@Composable
private fun ImmersiveSystemBarsEffect(
    controller: WindowInsetsControllerCompat,
) {
    DisposableEffect(controller) {
        val previousAppearance = SystemBarsAppearance(
            isLightStatusBars = controller.isAppearanceLightStatusBars,
            isLightNavigationBars = controller.isAppearanceLightNavigationBars,
        )

        controller.systemBarsBehavior = WindowInsetsControllerCompat
            .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = previousAppearance.isLightStatusBars
            controller.isAppearanceLightNavigationBars = previousAppearance.isLightNavigationBars
        }
    }
}

@Composable
private fun SystemBarsVisibilityEffect(
    controller: WindowInsetsControllerCompat,
    displayMode: PhotoViewerDisplayMode,
) {
    LaunchedEffect(controller, displayMode) {
        val systemBars = WindowInsetsCompat.Type.systemBars()

        when (displayMode) {
            PhotoViewerDisplayMode.Carousel -> controller.show(systemBars)
            PhotoViewerDisplayMode.Immersive -> controller.hide(systemBars)
        }
    }
}

private data class SystemBarsAppearance(
    val isLightStatusBars: Boolean,
    val isLightNavigationBars: Boolean,
)

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
