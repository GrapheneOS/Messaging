package com.android.messaging.di.debugmmsconfig

import com.android.messaging.data.debugmmsconfig.repository.MmsConfigRepository
import com.android.messaging.data.debugmmsconfig.repository.MmsConfigRepositoryImpl
import com.android.messaging.ui.debug.screen.mapper.DebugMmsConfigUiStateMapper
import com.android.messaging.ui.debug.screen.mapper.DebugMmsConfigUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DebugMmsConfigBindsModule {

    @Binds
    @Reusable
    abstract fun bindMmsConfigRepository(
        impl: MmsConfigRepositoryImpl,
    ): MmsConfigRepository

    @Binds
    @Reusable
    abstract fun bindDebugMmsConfigUiStateMapper(
        impl: DebugMmsConfigUiStateMapperImpl,
    ): DebugMmsConfigUiStateMapper
}
