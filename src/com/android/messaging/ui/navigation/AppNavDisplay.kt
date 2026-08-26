package com.android.messaging.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.android.messaging.ui.common.components.horizontalSlideContentTransform
import com.android.messaging.ui.common.components.predictiveBackContentTransform

@Composable
internal fun AppNavDisplay(
    backStack: MutableList<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    onBack: () -> Unit,
    sceneStrategies: List<SceneStrategy<NavKey>>,
    showsTwoPanes: Boolean,
    modifier: Modifier = Modifier,
) {
    val saveableStateHolderDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val viewModelStoreDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()
    val listDetailPaneDecorator = rememberListDetailPaneNavEntryDecorator(showsTwoPanes)
    val displayCornerDecorator = rememberDisplayCornerNavEntryDecorator()
    val paneTitleDecorator = rememberPaneTitleNavEntryDecorator()
    val entryDecorators = remember(
        saveableStateHolderDecorator,
        viewModelStoreDecorator,
        listDetailPaneDecorator,
        displayCornerDecorator,
        paneTitleDecorator,
    ) {
        listOf(
            saveableStateHolderDecorator,
            viewModelStoreDecorator,
            listDetailPaneDecorator,
            displayCornerDecorator,
            paneTitleDecorator,
        )
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.background,
        ),
        onBack = { onBack() },
        entryDecorators = entryDecorators,
        sceneStrategies = sceneStrategies,
        transitionSpec = { horizontalSlideContentTransform(isForward = true) },
        popTransitionSpec = { horizontalSlideContentTransform(isForward = false) },
        predictivePopTransitionSpec = { swipeEdge ->
            predictiveBackContentTransform(swipeEdge = swipeEdge)
        },
        entryProvider = entryProvider,
    )
}

@Composable
internal fun rememberAppSceneStrategies(
    additionalStrategies: List<SceneStrategy<NavKey>>,
): List<SceneStrategy<NavKey>> {
    return remember(additionalStrategies) {
        listOf(
            OverlaySceneStrategy(),
            DialogSceneStrategy(),
        ) + additionalStrategies
    }
}
