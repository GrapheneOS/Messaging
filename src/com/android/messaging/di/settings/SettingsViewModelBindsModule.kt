package com.android.messaging.di.settings

import com.android.messaging.ui.appsettings.general.delegate.AppSettingsDelegate
import com.android.messaging.ui.appsettings.general.delegate.AppSettingsDelegateImpl
import com.android.messaging.ui.appsettings.subscription.delegate.SubscriptionSettingsDelegate
import com.android.messaging.ui.appsettings.subscription.delegate.SubscriptionSettingsDelegateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class SettingsViewModelBindsModule {

    @Binds
    @ViewModelScoped
    abstract fun bindSubscriptionSettingsDelegate(
        impl: SubscriptionSettingsDelegateImpl,
    ): SubscriptionSettingsDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindAppSettingsDelegate(
        impl: AppSettingsDelegateImpl,
    ): AppSettingsDelegate
}
