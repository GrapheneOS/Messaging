package com.android.messaging.di.blockedparticipants

import com.android.messaging.ui.blockedparticipants.screen.delegate.BlockedParticipantsDelegate
import com.android.messaging.ui.blockedparticipants.screen.delegate.BlockedParticipantsDelegateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class BlockedParticipantsViewModelBindsModule {

    @Binds
    @ViewModelScoped
    abstract fun bindBlockedParticipantsDelegate(
        impl: BlockedParticipantsDelegateImpl,
    ): BlockedParticipantsDelegate
}
