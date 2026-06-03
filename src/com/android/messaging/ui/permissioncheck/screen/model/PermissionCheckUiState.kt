package com.android.messaging.ui.permissioncheck.screen.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class PermissionCheckUiState(
    val settingsGuidance: SettingsGuidance? = null,
    val missingPermissions: ImmutableList<String> = persistentListOf(),
)

internal enum class SettingsGuidance {
    DefaultSmsApp,
    Permissions,
}
