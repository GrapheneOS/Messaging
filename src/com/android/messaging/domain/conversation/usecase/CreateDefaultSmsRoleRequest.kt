package com.android.messaging.domain.conversation.usecase

import android.app.role.RoleManager
import android.content.Intent
import javax.inject.Inject

internal fun interface CreateDefaultSmsRoleRequest {
    operator fun invoke(): Intent?
}

internal class CreateDefaultSmsRoleRequestImpl @Inject constructor(
    private val roleManager: RoleManager,
) : CreateDefaultSmsRoleRequest {

    override operator fun invoke(): Intent? {
        return when {
            roleManager.isRoleAvailable(RoleManager.ROLE_SMS) -> {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            }

            else -> null
        }
    }
}
