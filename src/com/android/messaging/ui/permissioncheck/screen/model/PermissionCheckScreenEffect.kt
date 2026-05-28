package com.android.messaging.ui.permissioncheck.screen.model

import kotlinx.collections.immutable.ImmutableList

internal sealed interface PermissionCheckScreenEffect {

    data class RequestRuntimePermissions(
        val permissions: ImmutableList<String>,
    ) : PermissionCheckScreenEffect

    data object RequestSmsRole : PermissionCheckScreenEffect

    data object OpenAppSettings : PermissionCheckScreenEffect

    data object Redirect : PermissionCheckScreenEffect
}
