package com.android.messaging.di.classzero

import com.android.messaging.domain.classzero.usecase.SaveClassZeroMessage
import com.android.messaging.domain.classzero.usecase.SaveClassZeroMessageImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ClassZeroBindsModule {

    @Binds
    @Reusable
    fun bindSaveClassZeroMessage(
        impl: SaveClassZeroMessageImpl,
    ): SaveClassZeroMessage
}
