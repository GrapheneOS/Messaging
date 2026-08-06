package com.android.messaging.ui.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.android.messaging.ui.navigation.AppNavDisplay
import com.android.messaging.ui.navigation.NavigationReducer
import com.android.messaging.ui.navigation.NavigationReducerImpl
import com.android.messaging.ui.navigation.rememberNavigator

@Composable
internal fun AppNavGraph(
    startDestination: NavKey,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    navigationReducer: NavigationReducer = defaultNavigationReducer,
) {
    val backStack = rememberNavBackStack(startDestination)
    val navigator = rememberNavigator(
        backStack = backStack,
        navigationReducer = navigationReducer,
        onFinish = onFinish,
    )
    val entryProvider = remember(navigator) {
        appNavEntryProvider(navigator = navigator)
    }

    AppNavDisplay(
        backStack = backStack,
        entryProvider = entryProvider,
        onBack = navigator::back,
        modifier = modifier,
    )
}

private val defaultNavigationReducer: NavigationReducer = NavigationReducerImpl()
