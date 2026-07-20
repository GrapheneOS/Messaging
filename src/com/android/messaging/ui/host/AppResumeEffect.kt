package com.android.messaging.ui.host

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.navigation.Navigator
import com.android.messaging.ui.onboarding.navigation.OnboardingNavKey

@Composable
internal fun AppResumeEffect(
    backStack: List<NavKey>,
    navigator: Navigator,
    shouldShowOnboarding: () -> Boolean,
    onAppResumed: () -> Unit,
) {
    if (backStack.lastOrNull() == OnboardingNavKey) {
        return
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        if (shouldShowOnboarding()) {
            navigator.reset(destinations = listOf(OnboardingNavKey))
            return@LifecycleEventEffect
        }

        onAppResumed()
    }
}
