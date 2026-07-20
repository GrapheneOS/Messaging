package com.android.messaging.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation3.runtime.NavKey

@Stable
internal class Navigator(
    private val backStack: MutableList<NavKey>,
    private val navigationReducer: NavigationReducer,
    private val onFinish: () -> Unit,
) {

    fun reset(destination: NavKey) {
        navigationReducer.reset(
            backStack = backStack,
            destination = destination,
        )
    }

    fun back() {
        if (navigationReducer.pop(backStack = backStack)) {
            return
        }

        onFinish()
    }
}

@Composable
internal fun rememberNavigator(
    backStack: MutableList<NavKey>,
    navigationReducer: NavigationReducer,
    onFinish: () -> Unit,
): Navigator {
    val currentOnFinish = rememberUpdatedState(newValue = onFinish)

    return remember(backStack, navigationReducer) {
        Navigator(
            backStack = backStack,
            navigationReducer = navigationReducer,
            onFinish = { currentOnFinish.value() },
        )
    }
}
