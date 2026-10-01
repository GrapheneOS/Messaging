package com.android.messaging.di.conversation

import com.android.messaging.data.conversation.store.ConversationArchiveEvents
import com.android.messaging.data.conversation.store.ConversationArchiveEventsImpl
import com.android.messaging.ui.conversation.entry.ConversationLaunchStore
import com.android.messaging.ui.conversation.entry.ConversationLaunchStoreImpl
import com.android.messaging.ui.conversation.navigation.ConversationDraftLauncher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
internal abstract class ConversationLaunchModule {

    @Binds
    @ActivityRetainedScoped
    abstract fun bindConversationArchiveEvents(
        impl: ConversationArchiveEventsImpl,
    ): ConversationArchiveEvents

    @Binds
    abstract fun bindConversationLaunchStore(
        impl: ConversationLaunchStoreImpl,
    ): ConversationLaunchStore

    @Binds
    abstract fun bindConversationDraftLauncher(
        impl: ConversationLaunchStoreImpl,
    ): ConversationDraftLauncher
}
