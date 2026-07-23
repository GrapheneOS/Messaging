package com.android.messaging.ui.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.conversation.entry.ConversationEntryScreenModel
import com.android.messaging.ui.conversation.entry.ConversationEntryViewModel
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.navigation.ConversationEntryNavState
import com.android.messaging.ui.conversation.navigation.LocalConversationEntryNavState
import com.android.messaging.ui.conversation.navigation.conversationLaunchBackStack
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
    conversationRootDestinations: List<NavKey>,
    isLaunchedFromBubble: Boolean,
    initialLaunchRequest: ConversationEntryLaunchRequest?,
    launchRequests: Flow<ConversationEntryLaunchRequest?>,
    shouldShowOnboarding: () -> Boolean,
    onAppResumed: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    entryModel: ConversationEntryScreenModel = hiltViewModel<ConversationEntryViewModel>(),
    navigationReducer: NavigationReducer = defaultNavigationReducer,
) {
    val backStack = rememberAppBackStack(startDestinations = startDestinations)
    val navigator = rememberNavigator(
        backStack = backStack,
        navigationReducer = navigationReducer,
        onFinish = onFinish,
    )
    val entryProvider = remember { appNavEntryProvider() }
    val entryNavState = ConversationEntryNavState(
        model = entryModel,
        isLaunchedFromBubble = isLaunchedFromBubble,
    )

    AppResumeEffect(
        backStack = backStack,
        navigator = navigator,
        shouldShowOnboarding = shouldShowOnboarding,
        onAppResumed = onAppResumed,
    )

    AppNavLaunchEffects(
        initialLaunchRequest = initialLaunchRequest,
        launchRequests = launchRequests,
        onLaunchRequest = entryModel::onLaunchRequest,
        onLaunchBackStack = { launchRequest ->
            navigator.reset(
                destinations = conversationLaunchBackStack(
                    rootDestinations = conversationRootDestinations,
                    launchRequest = launchRequest,
                ),
            )
        },
    )

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalConversationEntryNavState provides entryNavState,
    ) {
        AppNavDisplay(
            backStack = backStack,
            entryProvider = entryProvider,
            onBack = navigator::back,
            modifier = modifier,
        )
    }
}

@Composable
internal fun AppNavLaunchEffects(
    initialLaunchRequest: ConversationEntryLaunchRequest?,
    launchRequests: Flow<ConversationEntryLaunchRequest?>,
    onLaunchRequest: (ConversationEntryLaunchRequest) -> Unit,
    onLaunchBackStack: (ConversationEntryLaunchRequest?) -> Unit,
) {
    LaunchedEffect(Unit) {
        initialLaunchRequest?.let(onLaunchRequest)
    }

    LaunchedEffect(launchRequests) {
        launchRequests.collect { launchRequest ->
            launchRequest?.let(onLaunchRequest)
            onLaunchBackStack(launchRequest)
        }
    }
}

private val defaultNavigationReducer: NavigationReducer = NavigationReducerImpl()
