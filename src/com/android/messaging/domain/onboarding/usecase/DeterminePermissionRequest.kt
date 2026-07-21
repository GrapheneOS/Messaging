package com.android.messaging.domain.onboarding.usecase

import com.android.messaging.data.onboarding.RequiredPermissionsChecker
import com.android.messaging.domain.onboarding.model.PermissionRequest
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
