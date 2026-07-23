package com.android.messaging.ui.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.conversation.navigation.ProvideConversationEntryNavState
import com.android.messaging.ui.navigation.AppNavDisplay
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.NavigationReducer
import com.android.messaging.ui.navigation.NavigationReducerImpl
import com.android.messaging.ui.navigation.rememberAppBackStack
import com.android.messaging.ui.navigation.rememberNavigator
import kotlinx.coroutines.flow.Flow

@Composable
internal fun AppNavGraph(
    startDestinations: List<NavKey>,
    isLaunchedFromBubble: Boolean,
    launchDestinations: Flow<List<NavKey>>,
    shouldShowOnboarding: () -> Boolean,
    onAppResumed: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    navigationReducer: NavigationReducer = defaultNavigationReducer,
) {
    val backStack = rememberAppBackStack(startDestinations = startDestinations)
    val navigator = rememberNavigator(
        backStack = backStack,
        navigationReducer = navigationReducer,
        onFinish = onFinish,
    )
    val entryProvider = remember { appNavEntryProvider() }

    AppResumeEffect(
        backStack = backStack,
        navigator = navigator,
        shouldShowOnboarding = shouldShowOnboarding,
        onAppResumed = onAppResumed,
    )

    AppNavLaunchEffects(
        launchDestinations = launchDestinations,
        onResetBackStack = navigator::reset,
    )

    CompositionLocalProvider(LocalNavigator provides navigator) {
        ProvideConversationEntryNavState(isLaunchedFromBubble = isLaunchedFromBubble) {
            AppNavDisplay(
                backStack = backStack,
                entryProvider = entryProvider,
                onBack = navigator::back,
                modifier = modifier,
            )
        }
    }
}

@Composable
internal fun AppNavLaunchEffects(
    launchDestinations: Flow<List<NavKey>>,
    onResetBackStack: (List<NavKey>) -> Unit,
) {
    LaunchedEffect(launchDestinations) {
        launchDestinations.collect(onResetBackStack)
    }
}

private val defaultNavigationReducer: NavigationReducer = NavigationReducerImpl()
