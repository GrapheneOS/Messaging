package com.android.messaging.di.blockedparticipants

import com.android.messaging.domain.blockedparticipants.usecase.SetDestinationBlocked
import com.android.messaging.domain.blockedparticipants.usecase.SetDestinationBlockedImpl
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
    abstract fun bindSetDestinationBlocked(
        impl: SetDestinationBlockedImpl,
    ): SetDestinationBlocked
}
