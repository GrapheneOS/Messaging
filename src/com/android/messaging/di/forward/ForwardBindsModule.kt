package com.android.messaging.di.forward

import com.android.messaging.domain.forward.usecase.BuildForwardConversationDraft
import com.android.messaging.domain.forward.usecase.BuildForwardConversationDraftImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ForwardBindsModule {

    @Binds
    @Reusable
    abstract fun bindBuildForwardConversationDraft(
        impl: BuildForwardConversationDraftImpl,
    ): BuildForwardConversationDraft
}
