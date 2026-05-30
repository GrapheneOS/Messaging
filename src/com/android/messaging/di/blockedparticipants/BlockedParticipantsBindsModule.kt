package com.android.messaging.di.blockedparticipants

import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepositoryImpl
import com.android.messaging.domain.blockedparticipants.usecase.DeleteDirectChats
import com.android.messaging.domain.blockedparticipants.usecase.DeleteDirectChatsImpl
import com.android.messaging.domain.blockedparticipants.usecase.SetDestinationBlocked
import com.android.messaging.domain.blockedparticipants.usecase.SetDestinationBlockedImpl
import com.android.messaging.ui.blockedparticipants.screen.mapper.BlockedParticipantsUiStateMapper
import com.android.messaging.ui.blockedparticipants.screen.mapper.BlockedParticipantsUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BlockedParticipantsBindsModule {

    @Binds
    @Reusable
    abstract fun bindBlockedParticipantsRepository(
        impl: BlockedParticipantsRepositoryImpl,
    ): BlockedParticipantsRepository

    @Binds
    @Reusable
    abstract fun bindBlockedParticipantsUiStateMapper(
        impl: BlockedParticipantsUiStateMapperImpl,
    ): BlockedParticipantsUiStateMapper

    @Binds
    @Reusable
    abstract fun bindSetDestinationBlocked(
        impl: SetDestinationBlockedImpl,
    ): SetDestinationBlocked

    @Binds
    @Reusable
    abstract fun bindDeleteDirectChats(
        impl: DeleteDirectChatsImpl,
    ): DeleteDirectChats
}
