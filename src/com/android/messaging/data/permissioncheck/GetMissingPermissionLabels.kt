package com.android.messaging.data.permissioncheck

import android.content.pm.PackageManager
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun interface GetMissingPermissionLabels {
    operator fun invoke(): ImmutableList<String>
}

internal class GetMissingPermissionLabelsImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val checker: RequiredPermissionsChecker,
) : GetMissingPermissionLabels {

    override fun invoke(): ImmutableList<String> {
        return checker.missingRequiredPermissions()
            .mapNotNull(::labelFor)
            .distinct()
            .toImmutableList()
    }

    private fun labelFor(permission: String): String? {
        val permissionInfo = runCatching {
            packageManager.getPermissionInfo(permission, 0)
        }.getOrNull() ?: return null

        val label = permissionInfo.loadLabel(packageManager).toString()
        return label.takeUnless { it == permission || it == permissionInfo.name }
    }
}
