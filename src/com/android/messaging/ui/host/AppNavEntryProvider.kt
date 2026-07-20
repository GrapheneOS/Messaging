package com.android.messaging.ui.host

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.android.messaging.ui.conversation.navigation.ConversationNavRouteState
import com.android.messaging.ui.conversation.navigation.conversationEntries
import com.android.messaging.ui.conversationlist.chats.ConversationListEffectHandlerImpl
import com.android.messaging.ui.conversationlist.chats.ConversationListScreen
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.navigation.Navigator
import com.android.messaging.ui.onboarding.navigation.OnboardingNavKey
import com.android.messaging.ui.onboarding.screen.OnboardingScreen
import com.android.messaging.ui.onboarding.screen.rememberOnboardingEffectHandler

internal fun appNavEntryProvider(
    navigator: Navigator,
    conversationRouteState: ConversationNavRouteState,
): (NavKey) -> NavEntry<NavKey> {
    return entryProvider {
        entry<ConversationListNavKey>(
            content = conversationListRouteContent(
                conversationRouteState = conversationRouteState,
            ),
        )
        entry<OnboardingNavKey>(
            content = onboardingRouteContent(navigator = navigator),
        )
        conversationEntries(routeState = conversationRouteState)
    }
}

private fun conversationListRouteContent(
    conversationRouteState: ConversationNavRouteState,
): @Composable (ConversationListNavKey) -> Unit {
    return {
        val activity = checkNotNull(LocalActivity.current)
        val hostView = LocalView.current
        val effectHandler = remember(activity, hostView) {
            ConversationListEffectHandlerImpl(
                activity = activity,
                hostView = hostView,
            )
        }

        ConversationListScreen(
            effectHandler = effectHandler,
            onNavigateToConversation = { conversationId ->
                conversationRouteState.navigationReducer.value.navigateToConversation(
                    backStack = conversationRouteState.backStack,
                    conversationId = conversationId,
                )
            },
            onNavigateToNewChat = {
                conversationRouteState.navigationReducer.value.navigateToNewChat(
                    backStack = conversationRouteState.backStack,
                )
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun onboardingRouteContent(
    navigator: Navigator,
): @Composable (OnboardingNavKey) -> Unit {
    return {
        OnboardingScreen(
            effectHandler = rememberOnboardingEffectHandler(),
            onNavigateBack = navigator::back,
            onOnboardingComplete = {
                navigator.reset(destinations = listOf(ConversationListNavKey))
            },
        )
    }
}
