package com.android.messaging.di.settings

import com.android.messaging.data.appsettings.repository.AppSettingsRepository
import com.android.messaging.data.appsettings.repository.AppSettingsRepositoryImpl
import com.android.messaging.data.subscriptionsettings.repository.SubscriptionSettingsRepository
import com.android.messaging.data.subscriptionsettings.repository.SubscriptionSettingsRepositoryImpl
import com.android.messaging.domain.subscriptionsettings.usecase.SetSubscriptionPhoneNumber
import com.android.messaging.domain.subscriptionsettings.usecase.SetSubscriptionPhoneNumberImpl
import com.android.messaging.ui.appsettings.general.mapper.AppSettingsUiStateMapper
import com.android.messaging.ui.appsettings.general.mapper.AppSettingsUiStateMapperImpl
import com.android.messaging.ui.appsettings.subscription.mapper.SubscriptionSettingsUiStateMapper
import com.android.messaging.ui.appsettings.subscription.mapper.SubscriptionSettingsUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsBindsModule {

    @Binds
    @Reusable
    abstract fun bindSubscriptionSettingsUiStateMapper(
        impl: SubscriptionSettingsUiStateMapperImpl,
    ): SubscriptionSettingsUiStateMapper

    @Binds
    @Reusable
    abstract fun bindAppSettingsUiStateMapper(
        impl: AppSettingsUiStateMapperImpl,
    ): AppSettingsUiStateMapper

    @Binds
    @Reusable
    abstract fun bindAppSettingsRepository(
        impl: AppSettingsRepositoryImpl,
    ): AppSettingsRepository

    @Binds
    @Reusable
    abstract fun bindSubscriptionSettingsRepository(
        impl: SubscriptionSettingsRepositoryImpl,
    ): SubscriptionSettingsRepository

    @Binds
    @Reusable
    abstract fun bindSetSubscriptionPhoneNumber(
        impl: SetSubscriptionPhoneNumberImpl,
    ): SetSubscriptionPhoneNumber
}
