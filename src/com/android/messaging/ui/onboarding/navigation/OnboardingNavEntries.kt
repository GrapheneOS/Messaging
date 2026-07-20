package com.android.messaging.ui.onboarding.navigation

import android.app.role.RoleManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.onboarding.screen.OnboardingEffectHandlerImpl
import com.android.messaging.ui.onboarding.screen.OnboardingScreen

internal fun EntryProviderScope<NavKey>.onboardingEntry(
    destinationsAfterOnboarding: List<NavKey>,
) {
    entry<OnboardingNavKey>(
        content = onboardingRouteContent(
            destinationsAfterOnboarding = destinationsAfterOnboarding,
        ),
    )
}

private fun onboardingRouteContent(
    destinationsAfterOnboarding: List<NavKey>,
): @Composable (OnboardingNavKey) -> Unit {
    return {
        val activity = checkNotNull(LocalActivity.current)
        val navigator = LocalNavigator.current
        val effectHandler = remember(activity) {
            OnboardingEffectHandlerImpl(
                activity = activity,
                roleManager = activity.getSystemService(RoleManager::class.java),
            )
        }

        OnboardingScreen(
            effectHandler = effectHandler,
            onNavigateBack = navigator::back,
            onOnboardingComplete = {
                navigator.reset(destinations = destinationsAfterOnboarding)
            },
        )
    }
}
