package com.android.messaging.di.core

import android.app.role.RoleManager
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.telephony.SubscriptionManager
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
internal class CoreProvidesModule {

    @Provides
    @Reusable
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher {
        return Dispatchers.Default
    }

    @Provides
    @Reusable
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    @Provides
    @Reusable
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher {
        return Dispatchers.Main
    }

    @Provides
    @Singleton
    @ApplicationCoroutineScope
    fun provideApplicationCoroutineScope(
        @DefaultDispatcher
        defaultDispatcher: CoroutineDispatcher,
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob() + defaultDispatcher)
    }

    @Provides
    @Reusable
    fun provideContentResolver(
        @ApplicationContext
        context: Context,
    ): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Reusable
    fun provideClipboardManager(
        @ApplicationContext
        context: Context,
    ): ClipboardManager {
        return context.getSystemService(ClipboardManager::class.java)
    }

    @Provides
    @Reusable
    fun provideRoleManager(
        @ApplicationContext
        context: Context,
    ): RoleManager {
        return context.getSystemService(RoleManager::class.java)
    }

    @Provides
    @Reusable
    fun provideSubscriptionManager(
        @ApplicationContext
        context: Context,
    ): SubscriptionManager {
        return context.getSystemService(SubscriptionManager::class.java)
    }
}
