package com.android.messaging.ui.onboarding.screen.model

import kotlinx.collections.immutable.ImmutableList

internal sealed interface OnboardingScreenEffect {

    data class RequestRuntimePermissions(
        val permissions: ImmutableList<String>,
    ) : OnboardingScreenEffect

    data object RequestSmsRole : OnboardingScreenEffect

    data object OpenAppSettings : OnboardingScreenEffect

    data object Redirect : OnboardingScreenEffect
}
