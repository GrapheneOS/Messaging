package com.android.messaging.di.devicepolicy

import com.android.messaging.data.devicepolicy.SmsRestrictionChecker
import com.android.messaging.data.devicepolicy.SmsRestrictionCheckerImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DevicePolicyBindsModule {

    @Binds
    @Reusable
    abstract fun bindSmsRestrictionChecker(
        impl: SmsRestrictionCheckerImpl,
    ): SmsRestrictionChecker
}
