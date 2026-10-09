package com.android.messaging.di.databasecompatibility

import com.android.messaging.data.databasecompatibility.DatabaseCompatibility
import com.android.messaging.data.databasecompatibility.DatabaseCompatibilityImpl
import com.android.messaging.data.databasecompatibility.store.DatabaseCompatibilityPreferencesStore
import com.android.messaging.data.databasecompatibility.store.DatabaseCompatibilityPreferencesStoreImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DatabaseCompatibilityBindsModule {

    @Binds
    @Reusable
    abstract fun bindDatabaseCompatibility(
        impl: DatabaseCompatibilityImpl,
    ): DatabaseCompatibility

    @Binds
    @Reusable
    abstract fun bindDatabaseCompatibilityPreferencesStore(
        impl: DatabaseCompatibilityPreferencesStoreImpl,
    ): DatabaseCompatibilityPreferencesStore
}
