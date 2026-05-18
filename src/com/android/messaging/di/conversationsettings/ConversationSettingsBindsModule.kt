package com.android.messaging.di.conversationsettings

import com.android.messaging.data.conversationsettings.repository.ConversationNotificationRepository
import com.android.messaging.data.conversationsettings.repository.ConversationNotificationRepositoryImpl
import com.android.messaging.data.conversationsettings.repository.ConversationSettingsRepository
import com.android.messaging.data.conversationsettings.repository.ConversationSettingsRepositoryImpl
import com.android.messaging.domain.conversationsettings.usecase.SetConversationArchived
import com.android.messaging.domain.conversationsettings.usecase.SetConversationArchivedImpl
import com.android.messaging.domain.conversationsettings.usecase.SetConversationDestinationBlocked
import com.android.messaging.domain.conversationsettings.usecase.SetConversationDestinationBlockedImpl
import com.android.messaging.domain.conversationsettings.usecase.SetConversationSelfParticipantId
import com.android.messaging.domain.conversationsettings.usecase.SetConversationSelfParticipantIdImpl
import com.android.messaging.ui.conversationsettings.screen.mapper.ConversationSettingsUiStateMapper
import com.android.messaging.ui.conversationsettings.screen.mapper.ConversationSettingsUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ConversationSettingsBindsModule {

    @Binds
    @Reusable
    abstract fun bindConversationSettingsUiStateMapper(
        impl: ConversationSettingsUiStateMapperImpl,
    ): ConversationSettingsUiStateMapper

    @Binds
    @Reusable
    abstract fun bindConversationSettingsRepository(
        impl: ConversationSettingsRepositoryImpl,
    ): ConversationSettingsRepository

    @Binds
    @Reusable
    abstract fun bindConversationNotificationRepository(
        impl: ConversationNotificationRepositoryImpl,
    ): ConversationNotificationRepository

    @Binds
    @Reusable
    abstract fun bindSetConversationArchived(
        impl: SetConversationArchivedImpl,
    ): SetConversationArchived

    @Binds
    @Reusable
    abstract fun bindSetConversationDestinationBlocked(
        impl: SetConversationDestinationBlockedImpl,
    ): SetConversationDestinationBlocked

    @Binds
    @Reusable
    abstract fun bindSetConversationSelfParticipantId(
        impl: SetConversationSelfParticipantIdImpl,
    ): SetConversationSelfParticipantId
}
