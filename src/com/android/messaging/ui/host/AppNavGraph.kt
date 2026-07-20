package com.android.messaging.ui.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.conversation.entry.ConversationEntryScreenModel
import com.android.messaging.ui.conversation.entry.ConversationEntryViewModel
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.navigation.ConversationNavRouteState
import com.android.messaging.ui.conversation.navigation.ConversationNavigationReducerImpl
import com.android.messaging.ui.conversation.navigation.conversationLaunchBackStack
import com.android.messaging.ui.navigation.AppNavDisplay
import com.android.messaging.ui.navigation.NavigationReducer
import com.android.messaging.ui.navigation.NavigationReducerImpl
import com.android.messaging.ui.navigation.rememberAppBackStack
import com.android.messaging.ui.navigation.rememberNavigator

@Composable
internal fun AppNavGraph(
    startDestinations: List<NavKey>,
    conversationRootDestinations: List<NavKey>,
    launchRequest: ConversationEntryLaunchRequest?,
    shouldShowOnboarding: () -> Boolean,
    onAppResumed: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    entryModel: ConversationEntryScreenModel = hiltViewModel<ConversationEntryViewModel>(),
    navigationReducer: NavigationReducer = defaultNavigationReducer,
) {
    val entryUiState by entryModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberAppBackStack(startDestinations = startDestinations)
    val processedLaunchGeneration = rememberSaveable {
        mutableStateOf(value = launchRequest?.launchGeneration)
    }
    val conversationReducer = remember(navigationReducer) {
        ConversationNavigationReducerImpl(navigationReducer = navigationReducer)
    }
    val navigator = rememberNavigator(
        backStack = backStack,
        navigationReducer = navigationReducer,
        onFinish = onFinish,
    )
    val conversationRouteState = ConversationNavRouteState(
        backStack = backStack,
        entryModel = rememberUpdatedState(newValue = entryModel),
        entryUiState = rememberUpdatedState(newValue = entryUiState),
        isLaunchedFromBubble = rememberUpdatedState(
            newValue = launchRequest?.isLaunchedFromBubble == true,
        ),
        navigationReducer = rememberUpdatedState(newValue = conversationReducer),
        onFinish = rememberUpdatedState(newValue = onFinish),
    )
    val entryProvider = remember(backStack) {
        appNavEntryProvider(
            navigator = navigator,
            conversationRouteState = conversationRouteState,
        )
    }

    AppResumeEffect(
        backStack = backStack,
        navigator = navigator,
        shouldShowOnboarding = shouldShowOnboarding,
        onAppResumed = onAppResumed,
    )

    AppNavGraphLaunchEffect(
        launchRequest = launchRequest,
        onLaunchRequest = entryModel::onLaunchRequest,
        onLaunchBackStackUpdate = { currentLaunchRequest ->
            applyLaunchToBackStack(
                backStack = backStack,
                conversationRootDestinations = conversationRootDestinations,
                launchRequest = currentLaunchRequest,
                navigationReducer = navigationReducer,
                processedLaunchGeneration = processedLaunchGeneration,
            )
        },
    )

    AppNavDisplay(
        backStack = backStack,
        entryProvider = entryProvider,
        onBack = navigator::back,
        modifier = modifier,
    )
}

@Composable
private fun AppNavGraphLaunchEffect(
    launchRequest: ConversationEntryLaunchRequest?,
    onLaunchRequest: (ConversationEntryLaunchRequest) -> Unit,
    onLaunchBackStackUpdate: (ConversationEntryLaunchRequest?) -> Unit,
) {
    val currentOnLaunchRequest = rememberUpdatedState(newValue = onLaunchRequest)
    val currentOnLaunchBackStackUpdate = rememberUpdatedState(newValue = onLaunchBackStackUpdate)

    LaunchedEffect(launchRequest) {
        launchRequest?.let(currentOnLaunchRequest.value)
        currentOnLaunchBackStackUpdate.value(launchRequest)
    }
}

private fun applyLaunchToBackStack(
    backStack: MutableList<NavKey>,
    conversationRootDestinations: List<NavKey>,
    launchRequest: ConversationEntryLaunchRequest?,
    navigationReducer: NavigationReducer,
    processedLaunchGeneration: MutableState<Int?>,
) {
    val launchGeneration = launchRequest?.launchGeneration

    if (processedLaunchGeneration.value == launchGeneration) {
        return
    }

    processedLaunchGeneration.value = launchGeneration
    navigationReducer.reset(
        backStack = backStack,
        destinations = conversationLaunchBackStack(
            rootDestinations = conversationRootDestinations,
            launchRequest = launchRequest,
        ),
    )
}

private val defaultNavigationReducer: NavigationReducer = NavigationReducerImpl()
