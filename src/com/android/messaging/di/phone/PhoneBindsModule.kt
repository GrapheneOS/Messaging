package com.android.messaging.di.phone

import com.android.messaging.data.phone.formatter.PhoneNumberFormatter
import com.android.messaging.data.phone.formatter.PhoneNumberFormatterImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PhoneBindsModule {

    @Binds
    @Reusable
    abstract fun bindPhoneNumberFormatter(
        impl: PhoneNumberFormatterImpl,
    ): PhoneNumberFormatter
}
