package com.android.messaging.ui.navigation

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.drawable.toDrawable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

internal object OverlayKey : NavMetadataKey<Unit>

internal fun overlayMetadata(): Map<String, Any> {
    return metadata {
        put(
            key = OverlayKey,
            value = Unit,
        )
    }
}

internal data class AppOverlayScene(
    override val key: Any,
    val entry: NavEntry<NavKey>,
    override val previousEntries: List<NavEntry<NavKey>>,
    override val overlaidEntries: List<NavEntry<NavKey>>,
    val onNavigateBack: () -> Unit,
) : OverlayScene<NavKey> {

    override val entries: List<NavEntry<NavKey>> = listOf(entry)

    override val content: @Composable () -> Unit = {
        Dialog(
            onDismissRequest = onNavigateBack,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            OverlayWindowEffect()
            entry.Content()
        }
    }
}

@Composable
private fun OverlayWindowEffect() {
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window

    SideEffect {
        dialogWindow?.apply {
            setDimAmount(0f)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            isNavigationBarContrastEnforced = false
        }
    }
}

internal class OverlaySceneStrategy : SceneStrategy<NavKey> {

    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>,
    ): Scene<NavKey>? {
        val lastEntry = entries.lastOrNull()
        val overlaidEntries = entries.dropLast(n = 1)
        val isOverlayEntry = lastEntry != null && OverlayKey in lastEntry.metadata

        if (!isOverlayEntry || overlaidEntries.isEmpty()) {
            return null
        }

        return AppOverlayScene(
            key = lastEntry.contentKey,
            entry = lastEntry,
            previousEntries = overlaidEntries,
            overlaidEntries = overlaidEntries,
            onNavigateBack = onBack,
        )
    }
}
