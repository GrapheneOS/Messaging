package com.android.messaging.domain.permissioncheck.usecase

import com.android.messaging.data.permissioncheck.RequiredPermissionsChecker
import com.android.messaging.domain.permissioncheck.model.PermissionRequest
import javax.inject.Inject

internal fun interface DeterminePermissionRequest {
    operator fun invoke(): PermissionRequest
}

internal class DeterminePermissionRequestImpl @Inject constructor(
    private val checker: RequiredPermissionsChecker,
) : DeterminePermissionRequest {

    override fun invoke(): PermissionRequest {
        val missing = checker.missingRequiredPermissions()
        return when {
            !checker.isSmsRoleHeld() -> PermissionRequest.SmsRole
            missing.isNotEmpty() -> PermissionRequest.RuntimePermissions(missing)
            else -> PermissionRequest.AlreadyGranted
        }
    }
}
