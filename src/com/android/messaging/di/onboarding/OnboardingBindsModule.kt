package com.android.messaging.di.onboarding

import com.android.messaging.data.onboarding.GetMissingPermissionLabels
import com.android.messaging.data.onboarding.GetMissingPermissionLabelsImpl
import com.android.messaging.data.onboarding.RequiredPermissionsChecker
import com.android.messaging.data.onboarding.RequiredPermissionsCheckerImpl
import com.android.messaging.data.onboarding.store.SmsWarningStore
import com.android.messaging.data.onboarding.store.SmsWarningStoreImpl
import com.android.messaging.domain.onboarding.usecase.DeterminePermissionRequest
import com.android.messaging.domain.onboarding.usecase.DeterminePermissionRequestImpl
import com.android.messaging.domain.onboarding.usecase.ShouldShowOnboarding
import com.android.messaging.domain.onboarding.usecase.ShouldShowOnboardingImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OnboardingBindsModule {

    @Binds
    @Reusable
    abstract fun bindRequiredPermissionsChecker(
        impl: RequiredPermissionsCheckerImpl,
    ): RequiredPermissionsChecker

    @Binds
    @Reusable
    abstract fun bindDeterminePermissionRequest(
        impl: DeterminePermissionRequestImpl,
    ): DeterminePermissionRequest

    @Binds
    @Reusable
    abstract fun bindGetMissingPermissionLabels(
        impl: GetMissingPermissionLabelsImpl,
    ): GetMissingPermissionLabels

    @Binds
    @Reusable
    abstract fun bindSmsWarningStore(
        impl: SmsWarningStoreImpl,
    ): SmsWarningStore

    @Binds
    @Reusable
    abstract fun bindShouldShowOnboarding(
        impl: ShouldShowOnboardingImpl,
    ): ShouldShowOnboarding
}
