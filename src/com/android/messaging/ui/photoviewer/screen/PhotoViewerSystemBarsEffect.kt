package com.android.messaging.ui.photoviewer.screen

import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode

private const val LIGHT_SYSTEM_BARS_APPEARANCE =
    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS

@Composable
internal fun PhotoViewerSystemBarsEffect(
    displayMode: PhotoViewerDisplayMode,
) {
    val view = LocalView.current

    ImmersiveSystemBarsEffect(view = view)

    SystemBarsVisibilityEffect(
        view = view,
        displayMode = displayMode,
    )
}

@Composable
private fun ImmersiveSystemBarsEffect(
    view: View,
) {
    DisposableEffect(view) {
        val controller = view.windowInsetsController ?: return@DisposableEffect onDispose { }

        val previousAppearance = controller.systemBarsAppearance and LIGHT_SYSTEM_BARS_APPEARANCE
        val previousBehavior = controller.systemBarsBehavior

        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.setSystemBarsAppearance(0, LIGHT_SYSTEM_BARS_APPEARANCE)

        onDispose {
            controller.show(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = previousBehavior
            controller.setSystemBarsAppearance(previousAppearance, LIGHT_SYSTEM_BARS_APPEARANCE)
        }
    }
}

@Composable
private fun SystemBarsVisibilityEffect(
    view: View,
    displayMode: PhotoViewerDisplayMode,
) {
    LaunchedEffect(view, displayMode) {
        val controller = view.windowInsetsController ?: return@LaunchedEffect
        val systemBars = WindowInsets.Type.systemBars()

        when (displayMode) {
            PhotoViewerDisplayMode.Carousel -> controller.show(systemBars)
            PhotoViewerDisplayMode.Immersive -> controller.hide(systemBars)
        }
    }
}
