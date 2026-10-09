package com.android.messaging.di.databaselifecycle

import com.android.messaging.data.databaselifecycle.DatabaseLifecycle
import com.android.messaging.data.databaselifecycle.DatabaseLifecycleImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DatabaseLifecycleBindsModule {

    @Binds
    @Reusable
    abstract fun bindDatabaseLifecycle(
        impl: DatabaseLifecycleImpl,
    ): DatabaseLifecycle
}
