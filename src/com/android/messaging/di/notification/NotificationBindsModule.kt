package com.android.messaging.di.notification

import com.android.messaging.domain.notification.usecase.MigrateConversationNotificationChannels
import com.android.messaging.domain.notification.usecase.MigrateConversationNotificationChannelsImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationBindsModule {

    @Binds
    @Reusable
    abstract fun bindMigrateConversationNotificationChannels(
        impl: MigrateConversationNotificationChannelsImpl,
    ): MigrateConversationNotificationChannels
}
