package com.android.messaging.di.conversation

import com.android.messaging.ui.conversation.entry.ConversationLaunchStore
import com.android.messaging.ui.conversation.entry.ConversationLaunchStoreImpl
import com.android.messaging.ui.conversation.navigation.ConversationDraftLauncher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent

@Module
@InstallIn(ActivityRetainedComponent::class)
internal abstract class ConversationLaunchModule {

    @Binds
    abstract fun bindConversationLaunchStore(
        impl: ConversationLaunchStoreImpl,
    ): ConversationLaunchStore

    @Binds
    abstract fun bindConversationDraftLauncher(
        impl: ConversationLaunchStoreImpl,
    ): ConversationDraftLauncher
}
