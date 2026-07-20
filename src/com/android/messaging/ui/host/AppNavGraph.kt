package com.android.messaging.ui.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.android.messaging.ui.navigation.Navigator
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
    val backStack = rememberAppBackStack(startDestinations = startDestinations)
    val processedLaunchGeneration = rememberSaveable {
        mutableStateOf(value = launchRequest?.launchGeneration)
    }
    val navigator = rememberNavigator(
        backStack = backStack,
        navigationReducer = navigationReducer,
        onFinish = onFinish,
    )
    val entryProvider = remember { appNavEntryProvider() }
    val entryNavState = ConversationEntryNavState(
        model = entryModel,
        isLaunchedFromBubble = launchRequest?.isLaunchedFromBubble == true,
    )

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
                navigator = navigator,
                conversationRootDestinations = conversationRootDestinations,
                launchRequest = currentLaunchRequest,
                processedLaunchGeneration = processedLaunchGeneration,
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
    navigator: Navigator,
    conversationRootDestinations: List<NavKey>,
    launchRequest: ConversationEntryLaunchRequest?,
    processedLaunchGeneration: MutableState<Int?>,
) {
    val launchGeneration = launchRequest?.launchGeneration

    if (processedLaunchGeneration.value == launchGeneration) {
        return
    }

    processedLaunchGeneration.value = launchGeneration
    navigator.reset(
        destinations = conversationLaunchBackStack(
            rootDestinations = conversationRootDestinations,
            launchRequest = launchRequest,
        ),
    )
}

private val defaultNavigationReducer: NavigationReducer = NavigationReducerImpl()
