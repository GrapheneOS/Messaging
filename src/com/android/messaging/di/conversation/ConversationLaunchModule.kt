package com.android.messaging.di.conversation

import com.android.messaging.data.conversation.event.ConversationArchiveEvents
import com.android.messaging.data.conversation.event.ConversationArchiveEventsImpl
import com.android.messaging.domain.conversation.usecase.action.ArchiveConversation
import com.android.messaging.domain.conversation.usecase.action.ArchiveConversationImpl
import com.android.messaging.ui.conversation.entry.ConversationLaunchStore
import com.android.messaging.ui.conversation.entry.ConversationLaunchStoreImpl
import com.android.messaging.ui.conversation.navigation.ConversationDraftLauncher
import dagger.Binds
import dagger.Module
import dagger.Reusable
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
    @Reusable
    abstract fun bindArchiveConversation(
        impl: ArchiveConversationImpl,
    ): ArchiveConversation

    @Binds
    abstract fun bindConversationLaunchStore(
        impl: ConversationLaunchStoreImpl,
    ): ConversationLaunchStore

    @Binds
    abstract fun bindConversationDraftLauncher(
        impl: ConversationLaunchStoreImpl,
    ): ConversationDraftLauncher
}
