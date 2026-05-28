package com.android.messaging.domain.permissioncheck.model

import kotlinx.collections.immutable.ImmutableList

internal sealed interface PermissionRequest {

    data object AlreadyGranted : PermissionRequest

    data object SmsRole : PermissionRequest

    data class RuntimePermissions(
        val permissions: ImmutableList<String>,
    ) : PermissionRequest
}
