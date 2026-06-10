package com.android.messaging.di.core

import com.android.messaging.data.debug.DebugFeaturesProvider
import com.android.messaging.data.debug.DebugFeaturesProviderImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CoreBindsModule {

    @Binds
    @Reusable
    abstract fun bindDebugFeaturesProvider(
        impl: DebugFeaturesProviderImpl,
    ): DebugFeaturesProvider
}
