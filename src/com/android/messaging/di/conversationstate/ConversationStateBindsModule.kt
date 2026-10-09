package com.android.messaging.di.conversationstate

import com.android.messaging.data.conversationstate.BlockedNumberRecorder
import com.android.messaging.data.conversationstate.BlockedNumberRecorderImpl
import com.android.messaging.data.conversationstate.ConversationStateMirror
import com.android.messaging.data.conversationstate.ConversationStateMirrorImpl
import com.android.messaging.data.conversationstate.store.ConversationStateDatabaseStore
import com.android.messaging.data.conversationstate.store.ConversationStateDatabaseStoreImpl
import com.android.messaging.data.conversationstate.store.ConversationStatePreferencesStore
import com.android.messaging.data.conversationstate.store.ConversationStatePreferencesStoreImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ConversationStateBindsModule {

    @Binds
    @Singleton
    abstract fun bindConversationStateMirror(
        impl: ConversationStateMirrorImpl,
    ): ConversationStateMirror

    @Binds
    @Reusable
    abstract fun bindConversationStatePreferencesStore(
        impl: ConversationStatePreferencesStoreImpl,
    ): ConversationStatePreferencesStore

    @Binds
    @Reusable
    abstract fun bindConversationStateDatabaseStore(
        impl: ConversationStateDatabaseStoreImpl,
    ): ConversationStateDatabaseStore

    @Binds
    @Reusable
    abstract fun bindBlockedNumberRecorder(
        impl: BlockedNumberRecorderImpl,
    ): BlockedNumberRecorder
}
