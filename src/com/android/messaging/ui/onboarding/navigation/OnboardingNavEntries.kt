package com.android.messaging.ui.onboarding.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.onboarding.screen.OnboardingScreen
import com.android.messaging.ui.onboarding.screen.rememberOnboardingEffectHandler

internal fun EntryProviderScope<NavKey>.onboardingEntries(
    destinationAfterOnboarding: NavKey,
) {
    entry<OnboardingNavKey>(
        content = onboardingRouteContent(
            destinationAfterOnboarding = destinationAfterOnboarding,
        ),
    )
}

private fun onboardingRouteContent(
    destinationAfterOnboarding: NavKey,
): @Composable (OnboardingNavKey) -> Unit {
    return {
        val navigator = LocalNavigator.current
        val effectHandler = rememberOnboardingEffectHandler()

        OnboardingScreen(
            effectHandler = effectHandler,
            onNavigateBack = navigator::back,
            onOnboardingComplete = {
                navigator.replaceTop(destination = destinationAfterOnboarding)
            },
        )
    }
}
