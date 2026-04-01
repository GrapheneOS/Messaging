package com.android.messaging.di.settings

import com.android.messaging.ui.appsettings.redesign.appsettings.delegate.AppSettingsDelegate
import com.android.messaging.ui.appsettings.redesign.appsettings.delegate.AppSettingsDelegateImpl
import com.android.messaging.ui.appsettings.redesign.appsettings.mapper.AppSettingsUiStateMapper
import com.android.messaging.ui.appsettings.redesign.appsettings.mapper.AppSettingsUiStateMapperImpl
import com.android.messaging.ui.appsettings.redesign.subscription.delegate.SubscriptionSettingsDelegate
import com.android.messaging.ui.appsettings.redesign.subscription.delegate.SubscriptionSettingsDelegateImpl
import com.android.messaging.ui.appsettings.redesign.subscription.mapper.SubscriptionSettingsUiStateMapper
import com.android.messaging.ui.appsettings.redesign.subscription.mapper.SubscriptionSettingsUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsBindsModule {

    @Binds
    abstract fun bindSubscriptionSettingsDelegate(
        impl: SubscriptionSettingsDelegateImpl,
    ): SubscriptionSettingsDelegate

    @Binds
    @Reusable
    abstract fun bindSubscriptionSettingsUiStateMapper(
        impl: SubscriptionSettingsUiStateMapperImpl,
    ): SubscriptionSettingsUiStateMapper

    @Binds
    abstract fun bindAppSettingsDelegate(
        impl: AppSettingsDelegateImpl,
    ): AppSettingsDelegate

    @Binds
    @Reusable
    abstract fun bindAppSettingsUiStateMapper(
        impl: AppSettingsUiStateMapperImpl,
    ): AppSettingsUiStateMapper
}
