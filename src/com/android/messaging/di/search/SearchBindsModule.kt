package com.android.messaging.di.search

import com.android.messaging.data.search.repository.MessageSearchRepository
import com.android.messaging.data.search.repository.MessageSearchRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SearchBindsModule {

    @Binds
    @Reusable
    abstract fun bindMessageSearchRepository(
        impl: MessageSearchRepositoryImpl,
    ): MessageSearchRepository
}
