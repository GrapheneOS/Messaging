package com.android.messaging.di.conversationlist

import com.android.messaging.ui.conversationlist.archived.mapper.ArchivedConversationListUiStateMapper
import com.android.messaging.ui.conversationlist.archived.mapper.ArchivedConversationListUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ArchivedConversationListBindsModule {

    @Binds
    @Reusable
    abstract fun bindArchivedConversationListUiStateMapper(
        impl: ArchivedConversationListUiStateMapperImpl,
    ): ArchivedConversationListUiStateMapper
}
