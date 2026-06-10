package com.android.messaging.di.conversationlist

import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.data.conversationlist.repository.ConversationListRepositoryImpl
import com.android.messaging.data.conversationlist.store.ConversationListStatusStore
import com.android.messaging.data.conversationlist.store.ConversationListStatusStoreImpl
import com.android.messaging.domain.conversationlist.usecase.DeleteConversations
import com.android.messaging.domain.conversationlist.usecase.DeleteConversationsImpl
import com.android.messaging.domain.conversationlist.usecase.SetConversationArchived
import com.android.messaging.domain.conversationlist.usecase.SetConversationArchivedImpl
import com.android.messaging.domain.conversationlist.usecase.SetConversationBlocked
import com.android.messaging.domain.conversationlist.usecase.SetConversationBlockedImpl
import com.android.messaging.ui.conversationlist.redesign.mapper.ConversationListUiStateMapper
import com.android.messaging.ui.conversationlist.redesign.mapper.ConversationListUiStateMapperImpl
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
    abstract fun bindDeleteConversations(
        impl: DeleteConversationsImpl,
    ): DeleteConversations

    @Binds
    @Reusable
    abstract fun bindSetConversationArchived(
        impl: SetConversationArchivedImpl,
    ): SetConversationArchived

    @Binds
    @Reusable
    abstract fun bindSetConversationBlocked(
        impl: SetConversationBlockedImpl,
    ): SetConversationBlocked
}
