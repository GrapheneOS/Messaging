package com.android.messaging.di.sync

import com.android.messaging.domain.sync.usecase.RepairAfterMessageSync
import com.android.messaging.domain.sync.usecase.RepairAfterMessageSyncImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SyncBindsModule {

    @Binds
    @Reusable
    abstract fun bindRepairAfterMessageSync(
        impl: RepairAfterMessageSyncImpl,
    ): RepairAfterMessageSync
}
