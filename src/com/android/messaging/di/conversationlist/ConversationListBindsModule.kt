package com.android.messaging.di.conversationlist

import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.data.conversationlist.repository.ConversationListRepositoryImpl
import com.android.messaging.data.conversationlist.store.ConversationListStatusStore
import com.android.messaging.data.conversationlist.store.ConversationListStatusStoreImpl
import com.android.messaging.ui.conversationlist.chats.mapper.ConversationListUiStateMapper
import com.android.messaging.ui.conversationlist.chats.mapper.ConversationListUiStateMapperImpl
import com.android.messaging.ui.conversationlist.mapper.ConversationListItemUiMapper
import com.android.messaging.ui.conversationlist.mapper.ConversationListItemUiMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ConversationListBindsModule {

    @Binds
    @Reusable
    abstract fun bindConversationListRepository(
        impl: ConversationListRepositoryImpl,
    ): ConversationListRepository

    @Binds
    @Reusable
    abstract fun bindConversationListStatusStore(
        impl: ConversationListStatusStoreImpl,
    ): ConversationListStatusStore

    @Binds
    @Reusable
    abstract fun bindConversationListUiStateMapper(
        impl: ConversationListUiStateMapperImpl,
    ): ConversationListUiStateMapper

    @Binds
    @Reusable
    abstract fun bindConversationListItemUiMapper(
        impl: ConversationListItemUiMapperImpl,
    ): ConversationListItemUiMapper
}
