package com.android.messaging.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

@Stable
internal interface Navigator {

    val backStack: MutableList<NavKey>

    fun push(destination: NavKey)

    fun replaceTop(destination: NavKey)

    fun reset(destinations: List<NavKey>)

    fun back()

    fun finish()
}

internal class NavigatorImpl(
    override val backStack: MutableList<NavKey>,
    private val navigationReducer: NavigationReducer,
    private val onFinish: () -> Unit,
) : Navigator {

    override fun push(destination: NavKey) {
        navigationReducer.push(
            backStack = backStack,
            destination = destination,
        )
    }

    override fun replaceTop(destination: NavKey) {
        navigationReducer.replaceTop(
            backStack = backStack,
            destination = destination,
        )
    }

    override fun reset(destinations: List<NavKey>) {
        navigationReducer.reset(
            backStack = backStack,
            destinations = destinations,
        )
    }

    override fun back() {
        if (navigationReducer.pop(backStack = backStack)) {
            return
        }

        onFinish()
    }

    override fun finish() {
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
        NavigatorImpl(
            backStack = backStack,
            navigationReducer = navigationReducer,
            onFinish = { currentOnFinish.value() },
        )
    }
}

internal val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No Navigator was provided")
}
