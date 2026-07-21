package com.android.messaging.ui.onboarding.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed interface OnboardingUiState {

    @Immutable
    data object SmsWarning : OnboardingUiState

    @Immutable
    data class Permissions(
        val settingsGuidance: SettingsGuidance? = null,
        val missingPermissions: ImmutableList<String> = persistentListOf(),
    ) : OnboardingUiState
}
