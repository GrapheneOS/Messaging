package com.android.messaging.domain.onboarding.usecase

import com.android.messaging.data.onboarding.RequiredPermissionsChecker
import com.android.messaging.data.onboarding.store.SmsWarningStore
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShouldShowOnboardingTest {

    @Test
    fun invoke_whenWarningPendingAndPermissionsGranted_returnsTrue() {
        val useCase = createUseCase(
            isAcknowledged = false,
            hasRequiredPermissions = true,
        )

        assertTrue(useCase())
    }

    @Test
    fun invoke_whenWarningAcknowledgedAndPermissionsMissing_returnsTrue() {
        val useCase = createUseCase(
            isAcknowledged = true,
            hasRequiredPermissions = false,
        )

        assertTrue(useCase())
    }

    @Test
    fun invoke_whenWarningPendingAndPermissionsMissing_returnsTrue() {
        val useCase = createUseCase(
            isAcknowledged = false,
            hasRequiredPermissions = false,
        )

        assertTrue(useCase())
    }

    @Test
    fun invoke_whenWarningAcknowledgedAndPermissionsGranted_returnsFalse() {
        val useCase = createUseCase(
            isAcknowledged = true,
            hasRequiredPermissions = true,
        )

        assertFalse(useCase())
    }

    private fun createUseCase(
        isAcknowledged: Boolean,
        hasRequiredPermissions: Boolean,
    ): ShouldShowOnboarding {
        val smsWarningStore = mockk<SmsWarningStore>().also {
            every { it.isAcknowledged() } returns isAcknowledged
        }
        val checker = mockk<RequiredPermissionsChecker>().also {
            every { it.hasRequiredPermissions() } returns hasRequiredPermissions
        }

        return ShouldShowOnboardingImpl(
            checker = checker,
            smsWarningStore = smsWarningStore,
        )
    }
}
