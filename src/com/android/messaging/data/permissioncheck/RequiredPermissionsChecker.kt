package com.android.messaging.data.permissioncheck

import android.app.role.RoleManager
import com.android.messaging.util.OsUtil
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface RequiredPermissionsChecker {
    fun hasRequiredPermissions(): Boolean
    fun isSmsRoleHeld(): Boolean
    fun missingRequiredPermissions(): ImmutableList<String>
}

internal class RequiredPermissionsCheckerImpl @Inject constructor(
    private val roleManager: RoleManager,
) : RequiredPermissionsChecker {

    override fun hasRequiredPermissions(): Boolean {
        return OsUtil.hasRequiredPermissions()
    }

    override fun isSmsRoleHeld(): Boolean {
        return roleManager.isRoleHeld(RoleManager.ROLE_SMS)
    }

    override fun missingRequiredPermissions(): ImmutableList<String> {
        return OsUtil.getMissingRequiredPermissions().toList().toImmutableList()
    }
}
