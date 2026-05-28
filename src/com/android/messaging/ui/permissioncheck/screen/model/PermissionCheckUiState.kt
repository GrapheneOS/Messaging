package com.android.messaging.ui.permissioncheck.screen.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class PermissionCheckUiState(
    val showSettingsGuidance: Boolean = false,
)
