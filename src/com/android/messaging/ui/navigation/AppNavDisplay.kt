package com.android.messaging.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay

@Composable
internal fun AppNavDisplay(
    backStack: MutableList<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sceneStrategy: SceneStrategy<NavKey> = SinglePaneSceneStrategy(),
) {
    val entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator<NavKey>(),
    )

    NavDisplay(
        backStack = backStack,
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.background,
        ),
        onBack = { onBack() },
        entryDecorators = entryDecorators,
        sceneStrategies = listOf(sceneStrategy),
        entryProvider = entryProvider,
    )
}
