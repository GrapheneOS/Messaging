package com.android.messaging.domain.onboarding.usecase

import com.android.messaging.data.onboarding.RequiredPermissionsChecker
import com.android.messaging.domain.onboarding.model.PermissionRequest
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

class DeterminePermissionRequestTest {

    @Test
    fun invoke_whenSmsRoleNotHeld_returnsSmsRole() {
        val checker = mockChecker(
            smsRoleHeld = false,
            missing = persistentListOf("android.permission.READ_SMS"),
        )

        val result = DeterminePermissionRequestImpl(checker)()

        assertEquals(PermissionRequest.SmsRole, result)
    }

    @Test
    fun invoke_whenSmsRoleHeldAndPermissionsMissing_returnsRuntimePermissions() {
        val missing = persistentListOf(
            "android.permission.READ_SMS",
            "android.permission.READ_CONTACTS",
        )
        val checker = mockChecker(
            smsRoleHeld = true,
            missing = missing,
        )

        val result = DeterminePermissionRequestImpl(checker)()

        assertEquals(PermissionRequest.RuntimePermissions(missing), result)
    }

    @Test
    fun invoke_whenSmsRoleHeldAndNothingMissing_returnsAlreadyGranted() {
        val checker = mockChecker(
            smsRoleHeld = true,
            missing = persistentListOf(),
        )

        val result = DeterminePermissionRequestImpl(checker)()

        assertEquals(PermissionRequest.AlreadyGranted, result)
    }

    private fun mockChecker(
        smsRoleHeld: Boolean,
        missing: ImmutableList<String>,
    ): RequiredPermissionsChecker {
        return mockk<RequiredPermissionsChecker>().also {
            every { it.isSmsRoleHeld() } returns smsRoleHeld
            every { it.missingRequiredPermissions() } returns missing
        }
    }
}
