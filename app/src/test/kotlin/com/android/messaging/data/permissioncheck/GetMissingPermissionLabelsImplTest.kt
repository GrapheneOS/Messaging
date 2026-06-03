package com.android.messaging.data.permissioncheck

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class GetMissingPermissionLabelsImplTest {

    private val packageManager = mockk<PackageManager>()
    private val checker = mockk<RequiredPermissionsChecker>()

    private val resolver = GetMissingPermissionLabelsImpl(packageManager, checker)

    @Test
    fun invoke_resolvesPermissionLabelsAndDedupes() {
        every { checker.missingRequiredPermissions() } returns persistentListOf(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_CONTACTS",
        )
        stubPermission("android.permission.READ_SMS", label = "read your text messages")
        stubPermission("android.permission.RECEIVE_SMS", label = "read your text messages")
        stubPermission("android.permission.READ_CONTACTS", label = "read your contacts")

        val labels = resolver()

        assertEquals(
            persistentListOf("read your text messages", "read your contacts"),
            labels,
        )
    }

    @Test
    fun invoke_whenLabelIsRawPermissionName_skipsIt() {
        val permission = "android.permission.READ_PHONE_STATE"
        every { checker.missingRequiredPermissions() } returns persistentListOf(permission)
        stubPermission(permission, label = permission)

        val labels = resolver()

        assertEquals(persistentListOf<String>(), labels)
    }

    @Test
    fun invoke_whenPermissionUnknown_skipsIt() {
        every { checker.missingRequiredPermissions() } returns persistentListOf(
            "android.permission.UNKNOWN",
        )
        every {
            packageManager.getPermissionInfo("android.permission.UNKNOWN", 0)
        } throws PackageManager.NameNotFoundException()

        val labels = resolver()

        assertEquals(persistentListOf<String>(), labels)
    }

    private fun stubPermission(permission: String, label: String) {
        val info = mockk<PermissionInfo>()
        every { info.loadLabel(packageManager) } returns label
        every { packageManager.getPermissionInfo(permission, 0) } returns info
    }
}
