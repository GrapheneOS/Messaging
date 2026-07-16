package com.android.messaging.domain.onboarding.usecase

import com.android.messaging.data.onboarding.RequiredPermissionsChecker
import com.android.messaging.data.onboarding.store.SmsWarningStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

internal interface ShouldShowOnboarding {
    operator fun invoke(): Boolean

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Provider {
        fun shouldShowOnboarding(): ShouldShowOnboarding
    }
}

internal class ShouldShowOnboardingImpl @Inject constructor(
    private val checker: RequiredPermissionsChecker,
    private val smsWarningStore: SmsWarningStore,
) : ShouldShowOnboarding {

    override fun invoke(): Boolean {
        val needsSmsWarning = !smsWarningStore.isAcknowledged()
        val needsPermissions = !checker.hasRequiredPermissions()

        return needsSmsWarning || needsPermissions
    }
}
