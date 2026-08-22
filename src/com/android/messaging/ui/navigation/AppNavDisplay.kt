package com.android.messaging.ui.navigation

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.android.messaging.ui.common.components.slideInFromLeft
import com.android.messaging.ui.common.components.slideInFromRight
import com.android.messaging.ui.common.components.slideOutToLeft
import com.android.messaging.ui.common.components.slideOutToRight

@Composable
internal fun AppNavDisplay(
    backStack: MutableList<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sceneStrategy: SceneStrategy<NavKey> = remember { SinglePaneSceneStrategy() },
) {
    val saveableStateHolderDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val viewModelStoreDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()
    val entryDecorators = remember(saveableStateHolderDecorator, viewModelStoreDecorator) {
        listOf(saveableStateHolderDecorator, viewModelStoreDecorator)
    }
    val sceneStrategies = remember(sceneStrategy) {
        listOf(sceneStrategy)
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.background,
        ),
        onBack = { onBack() },
        entryDecorators = entryDecorators,
        sceneStrategies = sceneStrategies,
        transitionSpec = { slideInFromRight() togetherWith slideOutToLeft() },
        popTransitionSpec = { slideInFromLeft() togetherWith slideOutToRight() },
        predictivePopTransitionSpec = { slideInFromLeft() togetherWith slideOutToRight() },
        entryProvider = entryProvider,
    )
}
